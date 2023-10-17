package com.uknowz.Service;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
        return taskDao.updateByPrimaryKeySelective(task);
    }

    //掌握完成
    private int taskDone(Task task) {
        task.setDone(1);
        return taskDao.updateByPrimaryKeySelective(task);
    }


    //获取下次复习时间
    private void setReviewTime(Task task) {
        long nextDuration = getNextDuration(task.getStage());
        Date date = new Date();
        date.setTime(date.getTime() + 1000 * nextDuration);

        //如果复习时间在0点~7点之间,统一在7点复习. 如果复习时间在其他时间,并且在大尺度时间后复习范畴, 统一10点推送复习内容
        LocalDateTime later = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        if (later.getHour() >= 0 && later.getHour() < 7) {
            later = later.withHour(7).withMinute(0).withSecond(0);
        } else {
            if(task.getStage() >= 5) {
                later = later.withHour(10).withMinute(0).withSecond(0);
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
        criteria.andLessThanOrEqualTo("reviewTime", now);
        criteria.andNotEqualTo("pic","");
        criteria.andEqualTo("sendMessageId", "");
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
        criteria.andLessThanOrEqualTo("reviewTime", DateUtil.offsetHour(now,-12));
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
        return taskDao.updateByPrimaryKeySelective(new Task() {{
            setSendMessageId(sendMessageId);
            setTaskId(taskId);
        }});
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
     *
     * @param text 搜索tag内容
     * @param openId 发送对象
     * @param type 0=查找随笔记录 1=查找记忆图片记录
     */
    public void selectTaskByTags(String text, String openId, Integer type) {
        Example example = new Example(Task.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("openId", openId);
        if(type ==0){
            criteria.andEqualTo("pic", "");
        }else {
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
                if (count ==0) {
                    condition.append("tags LIKE '%" + tag + "%'");
                }else {
                    condition.append("and tags LIKE '%" + tag + "%'");
                }
                count ++ ;
            }
        }


        criteria.andCondition("(title = '" + s[0] + "'" + (StringUtils.isEmpty(condition.toString())? "":"or(" +condition.toString() + ")") + ")");
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
        texts.add(0, "【查到结果,"+ "共" + texts.size() + "条,记忆图片查询可以回复对应id查看详情】~" );
        ImSample.sendTextMsg(FeishuLib.getClient(), openId, openId, "小可爱", false, texts);
    }

    public void sendTaskPicToUser(int taskId, String openId) throws Exception {
        Task task = taskDao.selectByPrimaryKey(taskId);
        if(task == null){
            ImSample.sendTextMsg(FeishuLib.getClient(), openId, openId, "小可爱", false, "没有查询到对应记忆图片");
            return;
        }
        ImSample.sendMonitorCardMsg(FeishuLib.getClient(), task.getOpenId(),task.getPic(),task.getTitle(),taskId + ":" +task.getContent(),"(阶段:" + (task.getStage() + 1) +")内容如下,对此卡片的回复无效");
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


}
