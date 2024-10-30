package com.uknowz.Service;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.lark.oapi.Client;
import com.uknowz.Common.BizResult;
import com.uknowz.Dao.TaskDao;
import com.uknowz.Pojo.DO.Memory.Task;
import com.uknowz.Service.ThirdLib.FeishuLib;
import com.uknowz.Service.ThirdLib.ImSample;
import org.apache.ibatis.session.RowBounds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tk.mybatis.mapper.entity.Example;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class MemoryService {
    //匹配标题
    String patternTitle = "\\[.*?\\]";
    //匹配标签
    String patternTags = "#.*?(\\s|$)";


    static int oneMin = 60;
    static int oneHour = 60 * 60;
    static int oneDay = 24 * 60 * 60;

    //定义不同阶段复习时间数组,单位为s
    //分别是在5分钟后重复一遍，20分钟后再重复一遍，1小时后，9小时后，1天后，2天后，5天后，8天后，14天后复习
    int[] SCHEDULE_TIME = {5 * oneMin, 20 * oneMin, oneHour, 9 * oneHour, oneDay, 2 * oneDay, 5 * oneDay, 8 * oneDay, 14 * oneDay};


    //每日复习最大数量(9stage以内不受控制)
    Long reviewMaxNumOneDay = 11l;


    //10level是否开启复习
    Boolean closeHighStage = true;

    @Autowired
    private TaskDao taskDao;
    @Autowired
    private FeishuLib feishuLib;

    //通过阶段获得下次复习时间
    public long getNextDuration(int stage) {

        if (stage >= SCHEDULE_TIME.length) {
            return 100 * oneDay;
        }

        return SCHEDULE_TIME[stage];
    }

    //保存文字任务
    public int saveTextTask(String openId, String unionId, String userId, String messageId, String text) throws Exception {
        Task task = new Task();
        task.setMessageId(messageId);
        task.setUserId(userId);
        task.setOpenId(openId);
        task.setUnionId(unionId);
        task.setDone(1);
        task.setStage(0);
        task.setMsgType(0);
        //获取回忆漫步权重最大值 最后计入的在权重上最后展示
        Example example = new Example(Task.class);
        example.selectProperties("showWeight");
        example.setOrderByClause("showWeight desc");
        RowBounds rowBounds = new RowBounds(0, 1);
        List<Task> tasks = taskDao.selectByExampleAndRowBounds(example, rowBounds);
        task.setShowWeight(tasks.get(0).getShowWeight());

        parseText(task, text);
        int i = saveTask(task);
        if (i < 1) {
            ImSample.replayMsg(feishuLib.getClient(), messageId, "接收失败了,请重新发送", "", "小可爱");
        } else {
            ImSample.replayMsg(feishuLib.getClient(), messageId, "已接收~", "", "小可爱");
        }
        return i;
    }

    //保存图片任务
    public int saveImgTask(String openId, String unionId, String userId, String messageId, String image_key) throws Exception {
        Task task = new Task();
        task.setMessageId(messageId);
        task.setUserId(userId);
        task.setOpenId(openId);
        task.setUnionId(unionId);
        task.setDone(0);
        task.setStage(0);
        task.setPic(image_key);
//                        task.setTitle();
//                        task.setTags();
        int i = saveTask(task);
        if (i < 1) {
            ImSample.replayMsg(feishuLib.getClient(), messageId, "接收失败了,请重新发送", "", "小可爱");
        } else {
            ImSample.replayMsg(feishuLib.getClient(), messageId, "已接收~", "", "小可爱");
        }
        return i;
    }


    //接收保存任务
    private int saveTask(Task task) {
        //设置下次复习时间
        setReviewTime(task);
        return taskDao.insertSelective(task);
    }

    //接收任务完成情况消息
    @Transactional(rollbackFor = Exception.class, value = "dbFirstTransactionManager")
    public BizResult recieveStatus(int done, Task task, String openMessageId, String openId) throws Exception {

        String content = "已记录,会准时提醒你复习哒~";
        if (done == 1) {//已掌握 不再复习
            taskDone(task);//完成
            content = "你真是个小天才~这个内容不会再出现~";
        } else {//没有完成
            taskUpdateStage(task);//提级
            content = content + "(下次复习时间:" + DateUtil.toLocalDateTime(task.getReviewTime()) + ")";
        }
        ImSample.replayMsg(feishuLib.getClient(), openMessageId, content, "", "");
        return BizResult.create("完成");

    }

    //提级
    private int taskUpdateStage(Task task) {
        task.setStage(task.getStage() + 1);
        //设置下次复习时间
        setReviewTime(task);
        return updateTask(task);
    }

    //掌握完成
    private int taskDone(Task task) {
        task.setDone(1);
        return updateTask(task);
    }


    //获取下次复习时间
    private void setReviewTime(Task task) {
        if (task.getMsgType() !=  null && task.getMsgType() == 2){
            return; //提醒时间在提醒分支已经处理好了
        }
        long nextDuration = getNextDuration(task.getStage());
        Date date = new Date();
        date.setTime(date.getTime() + 1000 * nextDuration);

        //如果复习时间在0点~7点之间,统一在7点复习. 如果复习时间在其他时间,并且在大尺度时间后复习范畴, 统一9:00点推送复习内容
        LocalDateTime later = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        if (later.getHour() >= 0 && later.getHour() < 7) {
            later = later.withHour(7).withMinute(0).withSecond(0);
        } else {
            if (task.getStage() >= 5) {
                later = later.withHour(9).withMinute(0).withSecond(0);
            }
        }
        Date result = Date.from(later.atZone(ZoneId.systemDefault()).toInstant());
        task.setReviewTime(result);
    }


    //获取待处理任务
    public List<Task> obterTaskList() {
        Date now = new Date();

        Example example = new Example(Task.class);
        example.setOrderByClause("createTime asc");
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("done", 0);
        criteria.andEqualTo("msgType", 1);
        criteria.andLessThanOrEqualTo("reviewTime", now);
        criteria.andEqualTo("sendMessageId", "");
        criteria.andNotEqualTo("pic", "");
        if (closeHighStage){
            criteria.andLessThan("stage",9);
        }
        List<Task> tasks = taskDao.selectByExample(example);

        return tasks;
    }

    //获取迟迟未处理的卡片(超过12小时还没有回复)
    public List<Task> obterTaskAlertList() {
        Date now = new Date();

        Example example = new Example(Task.class);
        example.setOrderByClause("createTime asc");
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("done", 0);
        criteria.andEqualTo("msgType", 1);
        criteria.andLessThanOrEqualTo("sendTime", DateUtil.offsetHour(now, -12));
        criteria.andNotEqualTo("sendMessageId", "");
        List<Task> tasks = taskDao.selectByExample(example);
        return tasks;
    }


    //对回复消息处理,更新图片的内容部分
    public void updateTaskContent(String openId, String messageId, String parentId, String text) throws Exception {
        //文字回复消息
        Task task = getTaskByMessageId(parentId);
        if (task == null) {
            //查询是否对复习内容进行回复
            task = getTaskBySendMessageId(parentId);
        }
        if (task == null) {//不支持
            ImSample.sendTextMsg(feishuLib.getClient(), openId, "", "", false, "我只是一个图片记录机器人~");
            return;
        }

        //获取内容
        parseText(task, text);
        updateTask(task);
        ImSample.replayMsg(feishuLib.getClient(), messageId, "内容已更新", "", "小可爱");
    }

    //解析文字消息 标题 内容 标签
    public void parseText(Task task, String text) {
        //[]中的内容是标题
        String title = parseTitle(text);

        //#空格中的内容是标签
        HashSet<String> tags = parseTags(text);

        //其余内容是文本内容

        //更新任务具体内容
        task.setTitle(title);
        task.setTags(JSONObject.toJSONString(tags));
        task.setContent(text);
    }

    private String parseTitle(String text) {
        String title = "";
        Pattern r = Pattern.compile(patternTitle);
        Matcher m = r.matcher(text);
        while (m.find()) {
            String group = m.group(0);
            title = group.replace("[", "").replace("]", "");
        }
        return title;
    }

    //解析标签tag
    private HashSet<String> parseTags(String text) {
        HashSet<String> tags = new HashSet<>();
        Pattern r2 = Pattern.compile(patternTags);
        Matcher m2 = r2.matcher(text);
        while (m2.find()) {
            String group = m2.group(0);
            String tag = group.replaceAll("[\\s|#]", "");
            tags.add(tag);
        }
        return tags;
    }


    public int updateTaskSendMessage(String sendMessageId, Integer taskId) {
        Task task = new Task();
        task.setSendMessageId(sendMessageId);
        task.setTaskId(taskId);
        task.setSendTime(new Date());
        return updateTask(task);
    }


    public Task getTaskBySendMessageId(String openMessageId) {
        List<Task> select = taskDao.select(new Task() {{
            setSendMessageId(openMessageId);
        }});
        if (CollectionUtil.isEmpty(select)) {
            return null;
        }
        return select.get(0);
    }

    public Task getTaskByMessageId(String messageId) {
        List<Task> select = taskDao.select(new Task() {{
            setMessageId(messageId);
        }});
        if (CollectionUtil.isEmpty(select)) {
            return null;
        }
        return select.get(0);
    }

    public int updateTask(Task task) {
        if (task.getTaskId() == null) {
            return 0;
        }
        return taskDao.updateByPrimaryKeySelective(task);
    }

    /**
     * @param text   搜索tag内容
     * @param openId 发送对象
     * @param type   0=查找随笔记录 1=查找记忆图片记录
     */
    public void selectTaskByTags(String text, String openId, Integer type) {
        Example example = new Example(Task.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("openId", openId);
        if (type == 0) {
            criteria.andEqualTo("pic", "");
        } else {
            criteria.andNotEqualTo("pic", "");
        }

        String[] s = text.split("\\s");

        if (s.length == 0) {
            return;
        }
        boolean needSearch = false;
        StringBuilder condition = new StringBuilder();
        int count = 0;
        for (String tag : s) {
            if (!StringUtils.isEmpty(tag)) {
                needSearch = true;
                if (count == 0) {
                    condition.append("tags LIKE '%" + tag + "%'");
                } else {
                    condition.append("and tags LIKE '%" + tag + "%'");
                }
                count++;
            }
        }


        criteria.andCondition("(title = '" + s[0] + "'" + (StringUtils.isEmpty(condition.toString()) ? "" : "or(" + condition.toString() + ")") + ")");
        example.orderBy("createTime").desc();

        if (!needSearch) {
            return;
        }

        PageHelper.startPage(1, 50);
        List<Task> tasks = taskDao.selectByExample(example);
        if (CollectionUtil.isEmpty(tasks)) {
            ImSample.sendTextMsg(FeishuLib.getClient(), openId, openId, "小可爱", false, "没有对应内容~");
            return;
        }
        List<String> texts = tasks.stream().map(task -> task.getTaskId() + ": " + task.getContent()).collect(Collectors.toList());
        texts.add(0, "【查到结果," + "共" + texts.size() + "条,记忆图片查询可以回复对应id查看详情】~");
        ImSample.sendTextMsg(FeishuLib.getClient(), openId, openId, "小可爱", false, texts);
    }

    public void sendTaskPicToUser(int taskId, String openId) throws Exception {
        Task task = taskDao.selectByPrimaryKey(taskId);
        if (task == null) {
            ImSample.sendTextMsg(FeishuLib.getClient(), openId, openId, "小可爱", false, "没有查询到对应记忆图片");
            return;
        }
        ImSample.sendMonitorCardMsg(FeishuLib.getClient(), task.getOpenId(), task.getPic(), task.getTitle(), taskId + ":" + task.getContent(), "(阶段:" + (task.getStage() + 1) + ")内容如下,对此卡片的回复无效");
    }

    public List<Task> obterRandomTaskList(String openId) {
        Example example = new Example(Task.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("openId", openId);
        criteria.andEqualTo("pic", "");
        example.setOrderByClause("showWeight asc, rand() asc");
        PageHelper.startPage(1, 10);
        List<Task> tasks = taskDao.selectByExample(example);
        if (CollectionUtil.isEmpty(tasks)) {
            return tasks;
        }
        List<Integer> taskIds = tasks.stream().map(Task::getTaskId).collect(Collectors.toList());
        taskDao.batchUpdateShowWeight(taskIds);

        return tasks;
    }

    //回忆漫步
    public void randomMemory(String openId) {
        List<Task> tasks = obterRandomTaskList(openId);
        if (!CollectionUtil.isEmpty(tasks)) {
            List<String> collect = tasks.stream().map(Task::getContent).collect(Collectors.toList());
            collect.add(0, "【回忆漫步】~");
            ImSample.sendTextMsg(FeishuLib.getClient(), tasks.get(0).getOpenId(), "", "", false, collect);
        }
    }


    //艾宾浩斯体系, 所有应该复习的内容全部重新推送
    public void clearMessageIds(String openId) throws Exception {
        Example example = new Example(Task.class);
        example.setOrderByClause("createTime asc");
        Example.Criteria criteria = example.createCriteria();
        criteria.andNotEqualTo("sendMessageId", "");
        criteria.andEqualTo("openId", openId);
        criteria.andNotEqualTo("pic", "");
        if (closeHighStage){
            criteria.andLessThan("stage",9);
        }
        List<Task> tasks = taskDao.selectByExample(example);
        sendReviewCards(tasks);
    }

    /**
     * 艾宾浩斯体系之发送复习卡片, 发送前判断今日已发送多少张卡片, 若卡片发送过多的情况下, stage大于等于9的卡片今日不发送, 明日会自动被发送的.
     *
     * @param tasks
     * @throws Exception
     */
    public void sendReviewCards(List<Task> tasks) throws Exception {
        HashMap<String,  Set<Integer>> storeOpenIdAndNum = new HashMap<>();

        Client client = feishuLib.getClient();
        for (Task task : tasks) {
            //所有今日任务
            Set<Integer> taskIdsToday = storeOpenIdAndNum.get(task.getOpenId());
            if (taskIdsToday == null) {
                Example example = new Example(Task.class);
                Example.Criteria criteria = example.createCriteria();
                criteria.andGreaterThan("sendTime", DateUtil.beginOfDay(new Date()));
                criteria.andLessThan("sendTime", DateUtil.endOfDay(new Date()));
                criteria.andEqualTo("openId", task.getOpenId());
                criteria.andNotEqualTo("pic", "");
                List<Task> tasksToday = taskDao.selectByExample(example);


                Example example2 = new Example(Task.class);
                Example.Criteria criteria2 = example2.createCriteria();
//                criteria2.andGreaterThan("reviewTime", DateUtil.beginOfDay(new Date()));
                criteria2.andLessThan("reviewTime", DateUtil.endOfDay(new Date()));
                criteria2.andEqualTo("openId", task.getOpenId());
                criteria2.andNotEqualTo("pic", "");
                if (closeHighStage){
                    criteria.andLessThan("stage",9);
                }
                List<Task> tasksToday2 = taskDao.selectByExample(example2);

                // 使用Stream API合并并去重
                List<Task> collect = Stream.concat(tasksToday.stream(), tasksToday2.stream())
                        .distinct()
                        .collect(Collectors.toList());


                //取出今日可展示的任务
                // 取出所有 stage <= 9 的任务
                List<Task> selectedTasks = collect.stream()
                        .filter(a -> a.getStage() < 9)
                        .collect(Collectors.toList());

                // 如果取出的数量小于15，再取出 stage > 9 的任务以补齐
                if (selectedTasks.size() < reviewMaxNumOneDay) {
                    List<Task> remainingTasks = collect.stream()
                            .filter(b -> b.getStage() >= 9)
                            .limit(reviewMaxNumOneDay - selectedTasks.size())
                            .collect(Collectors.toList());
                    selectedTasks.addAll(remainingTasks);
                }

                Set<Integer> taskIds = selectedTasks.stream()
                        .map(Task::getTaskId)
                        .collect(Collectors.toSet());

                taskIdsToday = taskIds;
                storeOpenIdAndNum.put(task.getOpenId(),taskIds);
            }

            //今天学不上了 明天再学
            if (!taskIdsToday.contains(task.getTaskId())) {
                task.setReviewTime(DateUtil.offsetDay(task.getReviewTime(), 1));//明天再学
                task.setSendMessageId("");
                updateTask(task);
                continue;
            }

            //正常发送
            String s = ImSample.sendInteractiveMonitorMsg(client, task.getOpenId(), task.getPic(), task.getTitle(), task.getContent(), "复习阶段:" + (task.getStage() + 1));
            int i = updateTaskSendMessage(s, task.getTaskId());
        }
    }



    /**提醒**/
    // 正则表达式模式，用于匹配 "提醒我X时间单位后干某件事情" 的输入
    private static final String RELATIVE_TIME_PATTERN =
            "提醒我(\\d+)(分钟|小时|天|月|年)后(.*)";

    // 正则表达式模式，用于匹配 "提醒我YYYY年MM月DD日干某件事情" 的输入
    private static final String ABSOLUTE_DATE_PATTERN =
            "提醒我(\\d{4})年(\\d{2})月(\\d{2})日(.*)";

    // 正则表达式模式，用于匹配 "提醒我YYYY年MM月DD日干某件事情" 的输入
    private static final String ABSOLUTE_DATE_PATTERN_1 =
            "提醒我(\\d{4})年(\\d{2})月(\\d{2})日(\\d{2})点(\\d{2})分(.*)";

    //提醒我做某件事情
    public int cueMeDoSth(String openId, String unionId, String userId, String messageId, String text) throws Exception {
        Task task = new Task();
        task.setMessageId(messageId);
        task.setUserId(userId);
        task.setOpenId(openId);
        task.setUnionId(unionId);
        task.setDone(0);
        task.setStage(0);
        task.setMsgType(2);

        //获取回忆漫步权重最大值 最后计入的在权重上最后展示
        Example example = new Example(Task.class);
        example.selectProperties("showWeight");
        example.setOrderByClause("showWeight desc");
        RowBounds rowBounds = new RowBounds(0, 1);
        List<Task> tasks = taskDao.selectByExampleAndRowBounds(example, rowBounds);
        task.setShowWeight(tasks.get(0).getShowWeight());

        int i1 = extractReminderInfo(task, text);
        if (i1 < 1){
            ImSample.replayMsg(feishuLib.getClient(), messageId, "格式匹配失败了", "", "小可爱");
            return i1;
        }

        int i = saveTask(task);
        if (i < 1) {
            ImSample.replayMsg(feishuLib.getClient(), messageId, "接收失败了,请重新发送", "", "小可爱");
        } else {
            ImSample.replayMsg(feishuLib.getClient(), messageId, "已接收~", "", "小可爱");
        }
        return i;
    }


    //获取提醒时间和内容
    public static int extractReminderInfo(Task task, String input) {

        // 匹配相对时间输入
        Pattern relativePattern = Pattern.compile(RELATIVE_TIME_PATTERN);
        Matcher relativeMatcher = relativePattern.matcher(input);

        if (relativeMatcher.matches()) {
            int timeValue = Integer.parseInt(relativeMatcher.group(1));
            String timeUnit = relativeMatcher.group(2);
            String taskStr = relativeMatcher.group(3).trim();

            System.out.println("相对时间提醒: " +
                    "时间值=" + timeValue + ", 时间单位=" + timeUnit + ", 任务=" + task);

            task.setContent(taskStr);
            task.setReviewTime(dealTime(timeValue,timeUnit));

            return 1;
        }

        // 匹配绝对日期输入
        Pattern absolutePattern1 = Pattern.compile(ABSOLUTE_DATE_PATTERN_1);
        Matcher absoluteMatcher1 = absolutePattern1.matcher(input);

        if (absoluteMatcher1.matches()) {
            int year = Integer.parseInt(absoluteMatcher1.group(1));
            int month = Integer.parseInt(absoluteMatcher1.group(2));
            int day = Integer.parseInt(absoluteMatcher1.group(3));
            int hour = Integer.parseInt(absoluteMatcher1.group(4));
            int min = Integer.parseInt(absoluteMatcher1.group(5));
            String taskStr = absoluteMatcher1.group(6).trim();

            // 将字符串日期转换为Date对象（这里假设提醒时间为当天的00:00:00）
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            try {
                Date reminderDate = sdf.parse(year + "-" + month + "-" + day + " " +hour +":"+  min + ":00");
                System.out.println("绝对日期提醒: " +
                        "日期=" + sdf.format(reminderDate) + ", 任务=" + task);

                task.setContent(taskStr);
                task.setReviewTime(reminderDate);
            } catch (ParseException e) {
                e.printStackTrace();
                System.out.println("日期解析失败");
            }

            return 1;
        }

        // 匹配绝对日期输入
        Pattern absolutePattern = Pattern.compile(ABSOLUTE_DATE_PATTERN);
        Matcher absoluteMatcher = absolutePattern.matcher(input);

        if (absoluteMatcher.matches()) {
            int year = Integer.parseInt(absoluteMatcher.group(1));
            int month = Integer.parseInt(absoluteMatcher.group(2));
            int day = Integer.parseInt(absoluteMatcher.group(3));
            String taskStr = absoluteMatcher.group(4).trim();

            // 将字符串日期转换为Date对象（这里假设提醒时间为当天的00:00:00）
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            try {
                Date reminderDate = sdf.parse(year + "-" + month + "-" + day);
                System.out.println("绝对日期提醒: " +
                        "日期=" + sdf.format(reminderDate) + ", 任务=" + task);

                task.setContent(taskStr);
                task.setReviewTime(reminderDate);
            } catch (ParseException e) {
                e.printStackTrace();
                System.out.println("日期解析失败");
            }

            return 1;
        }


        return 0;
    }


    //根据 时间单体 和 数值, 得到具体日期
    public static Date dealTime(int timeValue, String timeUnit) {
        // 创建一个Calendar实例，并设置为当前日期和时间
        Calendar calendar = Calendar.getInstance();

        switch (timeUnit) {
            case "分钟":
                calendar.add(Calendar.MINUTE, timeValue);
                break;
            case "小时":
                calendar.add(Calendar.HOUR, timeValue);
                break;
            case "天":
                calendar.add(Calendar.DATE, timeValue);
                break;
            case "月":
                calendar.add(Calendar.MONTH, timeValue);
            case "年":
                calendar.add(Calendar.YEAR, timeValue);
                break;
            default:
                throw new IllegalArgumentException("Unsupported time unit: " + timeUnit);
        }


        if (timeUnit.equals("分钟") || timeUnit.equals("小时") ){
            Date dateFiveMonthsLater = calendar.getTime();
            return dateFiveMonthsLater;
        }

        //跨天统一10点叫
        // 设置小时为10（24小时制）
        calendar.set(Calendar.HOUR_OF_DAY, 10);
        // 设置分钟为0（可选，如果你想要精确到分钟）
        calendar.set(Calendar.MINUTE, 0);
        // 设置秒为0（可选，如果你想要精确到秒）
        calendar.set(Calendar.SECOND, 0);
        // 设置毫秒为0（可选，如果你想要精确到毫秒）
        calendar.set(Calendar.MILLISECOND, 0);

        Date dateFiveMonthsLater = calendar.getTime();
        return dateFiveMonthsLater;
    }

    public void sendCueMeDOList(String openId) {
        Date now = new Date();

        Example example = new Example(Task.class);
        example.setOrderByClause("createTime asc");
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("msgType", 2);
        criteria.andEqualTo("done", 0);
        criteria.andLessThanOrEqualTo("reviewTime", now);
        criteria.andEqualTo("openId", openId);
        List<Task> tasks = taskDao.selectByExample(example);

        for (Task task:tasks){
            ImSample.sendTextMsg(FeishuLib.getClient(), tasks.get(0).getOpenId(), "", "", false, "小主你该做任务啦~",task.getContent());
            taskDone(task);
        }


    }
}
