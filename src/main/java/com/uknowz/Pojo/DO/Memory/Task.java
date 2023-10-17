package com.uknowz.Pojo.DO.Memory;

import lombok.Data;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

@Table(name = "flo_memory_task")
@Data
public class Task {
    @Id
    @GeneratedValue(generator = "JDBC", strategy = GenerationType.IDENTITY)
    private Integer taskId;
    private String messageId;//消息id 唯一
    private String sendMessageId;//复习消息id (会被覆盖)
    private Integer stage;//阶段
    private String title;//任务名称
    private String content;//任务内容
    private String pic;//任务图片
    private String tags;//任务标签json
    private String openId;//
    private String unionId;//
    private String userId;//用户id
    private Integer done;//0=未完成 1=完成
    private Integer showWeight;//回忆漫步权重

    private Date createTime;//创建时间
    private Date reviewTime;//下次复习时间(通过这个查找用户需要复习的东西)
}
