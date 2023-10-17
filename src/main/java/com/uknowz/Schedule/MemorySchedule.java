package com.uknowz.Schedule;

import com.lark.oapi.Client;
import com.uknowz.Pojo.DO.Memory.Task;
import com.uknowz.Service.MemoryService;
import com.uknowz.Service.ThirdLib.FeishuLib;
import com.uknowz.Service.ThirdLib.ImSample;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
public class MemorySchedule {

    @Autowired
    private FeishuLib feishuLib;

    @Autowired
    private MemoryService memoryService;

    private String[] candidates = {"张晓婷","张阅"};
    private LocalDate startDate = LocalDate.of(2023, 6, 26); // Replace with your own start date


    //工作辅助
    //每天九点半推送今天值班
//    @Scheduled(cron = "0 0/1 * * * ?")
    @Scheduled(cron = "0 40 9 * * ?")
    public void helpForJob() throws Exception {


        LocalDate currentDate = LocalDate.now();
        DayOfWeek currentDayOfWeek = currentDate.getDayOfWeek();
        if (currentDayOfWeek == DayOfWeek.SATURDAY || currentDayOfWeek == DayOfWeek.SUNDAY) {
            System.out.println("It's the weekend, no one is on duty.");
            return;
        }
        int daysBetween = 0;
        LocalDate tempDate = startDate;
        while (tempDate.isBefore(currentDate)) {
            if (tempDate.getDayOfWeek() != DayOfWeek.SATURDAY && tempDate.getDayOfWeek() != DayOfWeek.SUNDAY) {
                daysBetween++;
            }
            tempDate = tempDate.plusDays(1);
        }

        int index = daysBetween % candidates.length;
        String personOnDuty = candidates[index];
        ImSample.sendTextMsg(FeishuLib.getClient(),"ou_5beb69daf448702c6a98adf0a568dfc5", "", "", false, String.format("今日广告对接技术值班:%s",personOnDuty));

    }



    //艾宾浩斯复习
    private boolean start = true;
    //1分钟轮询
    @Scheduled(cron = "0 0/1 * * * ?")
    public void memoryReview() throws Exception {
        try {
            if (start) {
                start = false;
                Client client = feishuLib.getClient();
                List<Task> tasks = memoryService.obterTaskList();
                for (Task task : tasks) {
//                ImSample.sendImageMsg(client, task.getOpenId(), task.getPic());
                    String s = ImSample.sendInteractiveMonitorMsg(client, task.getOpenId(), task.getPic(), task.getTitle(), task.getContent(), "复习阶段:" + (task.getStage() + 1));
                    int i = memoryService.updateTaskSendMessage(s, task.getTaskId());
                }
            }
        }catch (Exception e){

        }finally {
            start = true;
        }

    }


    //随机漫游 每天一次
    private boolean randomReviewStart = true;
    @Scheduled(cron = "0 0 10 * * ?")
    public void randomReview() throws Exception {
        if (randomReviewStart) {
            randomReviewStart = false;
            memoryService.randomMemory("ou_5beb69daf448702c6a98adf0a568dfc5");
            randomReviewStart = true;
        }

    }

    //4小时轮询,如果有发送提示的卡片很久没有得到回应, 再次提醒
    @Scheduled(cron = "0 0 7-22/4 * * *")
    public void memoryReviewAlert() throws Exception {
        if (start) {
            start = false;
            Client client = feishuLib.getClient();
            List<Task> tasks = memoryService.obterTaskAlertList();
            for(Task task: tasks){
//                ImSample.sendImageMsg(client, task.getOpenId(), task.getPic());
                String s = ImSample.sendInteractiveMonitorMsg(client, task.getOpenId(),task.getPic(),task.getTitle(),task.getContent(),"本内容超12小时未完成复习,尽快完成哦(阶段:" + (task.getStage() + 1) +")");
                int i = memoryService.updateTaskSendMessage(s, task.getTaskId());
            }
            start = true;
        }

    }


}
