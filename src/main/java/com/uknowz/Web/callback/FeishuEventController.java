package com.uknowz.Web.callback;
import cn.hutool.core.util.NumberUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lark.oapi.card.CardActionHandler;
import com.lark.oapi.card.enums.MessageCardButtonTypeEnum;
import com.lark.oapi.card.model.*;
import com.lark.oapi.core.utils.Jsons;
import com.lark.oapi.event.EventDispatcher;
import com.lark.oapi.sdk.servlet.ext.ServletAdapter;
import com.lark.oapi.service.im.v1.ImService;
import com.lark.oapi.service.im.v1.model.P2MessageReceiveV1;
import com.uknowz.Pojo.DO.Memory.Task;
import com.uknowz.Service.MemoryService;
import com.uknowz.Service.ThirdLib.FeishuLib;
import com.uknowz.Service.ThirdLib.ImSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
public class FeishuEventController {

    private String EncryptKey = "f45DQ6uTqnxV6QgAmoz5kda5oZGw0j7y";
    private String VerificationToken = "zFkFBhoMW7XrUNksiYrfHfSHSLIQPlRe";

    // 2. 注入 ServletAdapter 示例
    @Autowired
    private ServletAdapter servletAdapter;

    @Autowired
    private FeishuLib feishuLib;

    @Autowired
    private MemoryService memoryService;

    private Map<String,Long> storeSth= new ConcurrentHashMap<String,Long>();

    private List<String> helpSimpleTips = new ArrayList<String>(){{
        add("【使用指南】");
        add("我是一个普普通通的记录小天才~");
        add("发送 /record/文字, 记录内容, 可以用 #标签 (后面需要空格) 打标签");
//        add("发送 /find/文字, 通过标签查找内容");
        add("发送 图片, 进入艾宾浩斯科学复习模式,定时推送复习卡片");
        add("发送 科学复习, 了解科学复习机制");
        add("发送 更多, 解锁更多隐藏功能");

    }};

    private List<String> helpTips = new ArrayList<String>(){{
        add("【更多姿势】");
                add("发送 /find/文字, 通过标签查找内容      eg:/find/生活 记账");
                add("发送 /findPic/文字, 通过标签查找记忆图片      eg:/findPic/生活 记账. 然后回复id号查看对应图片详情");

//                        add("发送 /record/文字, 记录内容, 可以用#和空格打标签      eg:/record/#生活 #记账 今日支出500rmb ");
//                            add("发送 图片, 进入艾宾浩斯科学复习模式,定时推送复习卡片");
                                        add("对图片进行回复, 补充任务内容, 格式为[标题]#tag 内容      eg:回复图片: [谁动了我的奶酪]#励志 #读书 #英文 这本书的这页单词内容值得我反复揣摩");
//                                                add("发送 科学复习, 了解科学复习机制");
                                                    add("发送 回忆漫步, 随机10条你的(非图片)记录展现给你~");
    }};

    private List<String> scientificTips = new ArrayList<String>(){{
        add("【科学复习】~");
                add("艾宾浩斯记忆曲线由心理学家赫尔曼·艾宾浩斯通过自己1880年到1885年的实验提出的用于表述记忆中的中长期记忆的遗忘率的一种曲线" );
                add("通常情况下，记忆率如下：");
                        add("20分钟后，42%被遗忘掉，58%被记住。");
                                add("1小时后，56%被遗忘掉，44%被记住。");
                                        add("1天后，74%被遗忘掉，26%被记住。");
                                                add("1周后，77%被遗忘掉，23%被记住。");
                                                        add("1个月后，79%被遗忘掉，21%被记住。");
                                                                add("通过安排复习可以不同程度的保存记忆程度，科学的复习计划是在5分钟后重复一遍，20分钟后再重复一遍，1小时后，9小时后，1天后，2天后，5天后，8天后，14天后就会记得很牢。");
                                                                        add("给我发送图片, 我会在科学的复习节点为你推送复习内容, 助你轻松记忆~(备注:选择我学会了即不会再推送了哦~)");
    }};


    //1. 注册卡片处理器
    private final CardActionHandler CARD_ACTION_HANDLER = CardActionHandler.newBuilder(VerificationToken, EncryptKey,
            new CardActionHandler.ICardHandler() {
                @Override
                public Object handle(CardAction cardAction) throws Exception {
                    // 1.1 处理卡片行为
                    System.out.println(Jsons.DEFAULT.toJson(cardAction));
                    System.out.println(cardAction.getRequestId());

                    String openId = cardAction.getOpenId();
                    Action action = cardAction.getAction();
                    Map<String, Object> value = action.getValue();
                    Integer done = Integer.valueOf((String)value.get("done"));
                    String openMessageId = cardAction.getOpenMessageId();//消息对应

                    Task task = memoryService.getTaskBySendMessageId(openMessageId);
                    if (task == null){
//                        ImSample.sendTextMsg(feishuLib.getClient() ,openId , openId,"小可爱",false,"消息已过期");
                        ImSample.replayMsg(feishuLib.getClient() ,openMessageId ,"消息已过期", "","小可爱");

                        return null;
                    }else{
                        task.setSendMessageId("");
                        memoryService.recieveStatus(done,task,openMessageId, openId);
//                        ImSample.sendTextMsg(feishuLib.getClient() ,openId , openId,"小可爱",false,"已记录,会准时提醒你复习哒");
                        return null;
                    }

                }
            }).build();

    //2. 注册消息处理器
    private final EventDispatcher EVENT_DISPATCHER = EventDispatcher.newBuilder(VerificationToken,
            EncryptKey)
            .onP2MessageReceiveV1(new ImService.P2MessageReceiveV1Handler() {
                @Override
                public void handle(P2MessageReceiveV1 event) throws Exception {
                    System.out.println(Jsons.DEFAULT.toJson(event));
                    System.out.println(event.getRequestId());
                    String openId = event.getEvent().getSender().getSenderId().getOpenId();
                    String unionId = event.getEvent().getSender().getSenderId().getUnionId();
                    String userId = event.getEvent().getSender().getSenderId().getUserId();
                    String messageId = event.getEvent().getMessage().getMessageId();//消息id
                    String parentId = event.getEvent().getMessage().getParentId();//回复消息

                    Task taskByMessageId = memoryService.getTaskByMessageId(messageId);
                    if(taskByMessageId != null){//已接收
                        return;
                    }
                    String content = event.getEvent().getMessage().getContent();
                    JSONObject jsonObject = JSON.parseObject(content);

                    if (event.getEvent().getMessage().getMessageType().equals("image")) {
                        LocalTime now = LocalTime.now();
                        if (now.isAfter(LocalTime.of(22, 30))) {
                            ImSample.sendTextMsg(feishuLib.getClient(), openId,"","",false,"首波复习会超过12点,不利于大脑对知识的消化和存储,赶紧去睡觉哦~");
                            return;
                        }

                        String image_key = jsonObject.getString("image_key");//图片key
                        //图片消息 艾宾浩斯记忆曲线
                        memoryService.saveImgTask(openId, unionId, userId, messageId, image_key);

                    }else if (event.getEvent().getMessage().getMessageType().equals("text")) {
                        String text = jsonObject.getString("text");
                        if(parentId != null) {//回复消息 完善普图片消息内容
                            memoryService.updateTaskContent(openId, messageId, parentId, text);
                        }else{//文字消息
                            if (text.startsWith("/find/")) {//搜索标签 /find/我们 他们
                                String replace = text.replace("/find/", "");
                                memoryService.selectTaskByTags(replace,openId, 0);
                            }else if(text.startsWith("/record/")){//文字记录  不进行定时推送复习 仅仅可用标签查询
                                String replace = text.replace("/record/", "");
                                memoryService.saveTextTask(openId, unionId, userId, messageId, replace);
                            }else if(text.startsWith("/findPic/")){//搜索图片
                                String replace = text.replace("/findPic/", "");
                                memoryService.selectTaskByTags(replace,openId, 1);
                            }else if ("科学复习".equals(text)){
                                ImSample.sendTextMsg(feishuLib.getClient(), openId,"","",false,scientificTips);
                            }else if ("回忆漫步".equals(text)){
                                memoryService.randomMemory(openId);
                            }else if ("更多".equals(text)){
                                ImSample.sendTextMsg(feishuLib.getClient(), openId,"","",false,helpTips);
                            }else if(NumberUtil.isNumber(text) && NumberUtil.isInteger(text)){//回复序号 查找对应记忆图片返回用户
                                memoryService.sendTaskPicToUser(Integer.parseInt(text), openId);
                            }else if ("重发".equals(text)){//把所有目前待学习卡片重新推送
                                if(isRequestAllowed("resendTime")) {
                                    memoryService.clearMessageIds(openId);
                                }
                            }else {
                                ImSample.sendTextMsg(feishuLib.getClient(), openId,"","",false,helpSimpleTips);
                            }
                        }
                    }else{
                        ImSample.sendTextMsg(feishuLib.getClient(), openId,"","",false,helpSimpleTips);
                    }

                }
            })
//            .onP2UserCreatedV3(new ContactService.P2UserCreatedV3Handler() {
//                @Override
//                public void handle(P2UserCreatedV3 event) {
//                    System.out.println(Jsons.DEFAULT.toJson(event));
//                    System.out.println(event.getRequestId());
//                }
//            })
//            .onP2MessageReadV1(new ImService.P2MessageReadV1Handler() {
//                @Override
//                public void handle(P2MessageReadV1 event) {
//                    System.out.println(Jsons.DEFAULT.toJson(event));
//                    System.out.println(event.getRequestId());
//                }
//            })
            .build();


    //3. 注册服务路由
    @RequestMapping("/webhook/card")
    public void card(HttpServletRequest request, HttpServletResponse response)
            throws Throwable {
        //3.1 回调扩展包卡片行为处理回调
        servletAdapter.handleCardAction(request, response, CARD_ACTION_HANDLER);
    }

    //3. 创建路由处理器
    @RequestMapping("/webhook/event")
    public void event(HttpServletRequest request, HttpServletResponse response)
            throws Throwable {
        //3.1 回调扩展包提供的事件回调处理器
        servletAdapter.handleEvent(request, response, EVENT_DISPATCHER);
    }


    // 构建卡片响应
    private MessageCard getCard() {
        // 配置
        MessageCardConfig config = MessageCardConfig.newBuilder()
                .enableForward(true)
                .wideScreenMode(true)
                .updateMulti(true)
                .build();

        // cardUrl
        MessageCardURL cardURL = MessageCardURL.newBuilder()
                .pcUrl("http://www.baidu.com")
                .iosUrl("http://www.google.com")
                .url("http://open.feishu.com")
                .androidUrl("http://www.jianshu.com")
                .build();

        // header
        MessageCardHeader header = MessageCardHeader.newBuilder()
                .template("red")
                .title(MessageCardPlainText.newBuilder()
                        .content("1 级报警 - 数据平台")
                        .build())
                .build();

        //elements
        MessageCardDiv div1 = MessageCardDiv.newBuilder()
                .fields(new MessageCardField[]{
                        MessageCardField.newBuilder()
                                .isShort(true)
                                .text(MessageCardLarkMd.newBuilder()
                                        .content("**🕐 时间：**2021-02-23 20:17:51")
                                        .build())
                                .build(),
                        MessageCardField.newBuilder()
                                .isShort(true)
                                .text(MessageCardLarkMd.newBuilder()
                                        .content("**🔢 事件 ID：：**336720")
                                        .build())
                                .build(),
                        MessageCardField.newBuilder()
                                .isShort(false)
                                .text(MessageCardLarkMd.newBuilder()
                                        .content("")
                                        .build())
                                .build(),
                        MessageCardField.newBuilder()
                                .isShort(true)
                                .text(MessageCardLarkMd.newBuilder()
                                        .content("**📋 项目：**\nQA 7")
                                        .build())
                                .build(),
                        MessageCardField.newBuilder()
                                .isShort(true)
                                .text(MessageCardLarkMd.newBuilder()
                                        .content("**👤 一级值班：**\n<at id=ou_c245b0a7dff2725cfa2fb104f8b48b9d>加多</at>")
                                        .build())
                                .build(),
                        MessageCardField.newBuilder()
                                .isShort(false)
                                .text(MessageCardLarkMd.newBuilder()
                                        .content("")
                                        .build())
                                .build(),
                        MessageCardField.newBuilder()
                                .isShort(true)
                                .text(MessageCardLarkMd.newBuilder()
                                        .content("**👤 二级值班：**\n<at id=ou_c245b0a7dff2725cfa2fb104f8b48b9d>加多</at>")
                                        .build())
                                .build()
                })
                .build();

        MessageCardImage image = MessageCardImage.newBuilder()
                .alt(MessageCardPlainText.newBuilder()
                        .content("")
                        .build())
                .imgKey("img_v2_8b2ebeaf-c97c-411d-a4dc-4323e8cba10g")
                .title(MessageCardLarkMd.newBuilder()
                        .content("支付方式 支付成功率低于 50%：")
                        .build())
                .build();

        MessageCardNote note = MessageCardNote.newBuilder()
                .elements(new IMessageCardNoteElement[]{MessageCardPlainText.newBuilder()
                        .content("🔴 支付失败数  🔵 支付成功数")
                        .build()})
                .build();

        Map<String, Object> value = new HashMap<>();
        value.put("key1", "value1");
        MessageCardAction cardAction = MessageCardAction.newBuilder()
                .actions(new IMessageCardActionElement[]{
                        MessageCardEmbedButton.newBuilder()
                                .buttonType(MessageCardButtonTypeEnum.PRIMARY)
                                .value(value)
                                .text(MessageCardPlainText.newBuilder().content("跟进处理").build())
                                .build(),
                        MessageCardEmbedSelectMenuStatic.newBuilder()
                                .options(new MessageCardEmbedSelectOption[]{
                                        MessageCardEmbedSelectOption.newBuilder()
                                                .value("1")
                                                .text(MessageCardPlainText.newBuilder()
                                                        .content("屏蔽10分钟")
                                                        .build())
                                                .build(),
                                        MessageCardEmbedSelectOption.newBuilder()
                                                .value("2")
                                                .text(MessageCardPlainText.newBuilder()
                                                        .content("屏蔽30分钟")
                                                        .build())
                                                .build(),
                                        MessageCardEmbedSelectOption.newBuilder()
                                                .value("3")
                                                .text(MessageCardPlainText.newBuilder()
                                                        .content("屏蔽1小时")
                                                        .build())
                                                .build(),
                                        MessageCardEmbedSelectOption.newBuilder()
                                                .value("4")
                                                .text(MessageCardPlainText.newBuilder()
                                                        .content("屏蔽24小时")
                                                        .build())
                                                .build()
                                })
                                .placeholder(MessageCardPlainText.newBuilder()
                                        .content("暂时屏蔽报警")
                                        .build())
                                .value(value)
                                .build()
                })
                .build();

        MessageCardHr hr = MessageCardHr.newBuilder().build();

        MessageCardDiv div2 = MessageCardDiv.newBuilder()
                .text(MessageCardLarkMd.newBuilder()
                        .content(
                                "🙋🏼 [我要反馈误报](https://open.feishu.cn/) | 📝 [录入报警处理过程](https://open.feishu.cn/)")
                        .build())
                .build();

        MessageCard card = MessageCard.newBuilder()
                .cardLink(cardURL)
                .config(config)
                .header(header)
                .elements(new MessageCardElement[]{div1, note, image, cardAction, hr, div2})
                .build();

        return card;
    }

    // 构建自定义响应
    private CustomResponse getCustomResp(String value) {
        Map<String, Object> map = new HashMap<>();
        map.put("key1", value);
        map.put("ke2", "value2");
        CustomResponse customResponse = new CustomResponse();
        customResponse.setStatusCode(200);
        customResponse.setBody(map);
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        headers.put("key1", Arrays.asList("a", "b"));
        headers.put("key2", Arrays.asList("c", "d"));
        customResponse.setHeaders(headers);
        return customResponse;
    }

    //是否允许执行业务
    public boolean isRequestAllowed(String key) {
        long currentTime = System.currentTimeMillis();

        // 检查是否存在 key，并获取其对应的时间戳
        Long storedTime = storeSth.get(key);

        if (storedTime == null) {
            // 如果 key 不存在，允许请求，并将当前时间存储为时间戳
            storeSth.put(key, currentTime);
            return true;
        } else {
            // 如果 key 存在，检查时间是否超过1分钟
            long elapsedTime = currentTime - storedTime;
            if (elapsedTime >= 60000 * 60 *24) { // 1分钟 = 60,000毫秒
                // 如果超过1分钟，允许请求，并更新时间戳
                storeSth.put(key, currentTime);
                return true;
            } else {
                // 如果未超过1分钟，不允许请求
                return false;
            }
        }
    }

}
