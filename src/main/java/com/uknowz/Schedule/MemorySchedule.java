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
    private MemoryService memoryService;

    private String[] candidates = {"张阅","张晓婷"};
    private LocalDate startDate = LocalDate.of(2023, 10, 5); // Replace with your own start date


    //工作辅助
    //每天九点半推送今天值班
//    @Scheduled(cron = "0 0/1 * * * ?")
    @Scheduled(cron = "0 30 10 * * ?")
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
        if (start) {
            start = false;
            try {
                List<Task> tasks = memoryService.obterTaskList();
                memoryService.sendReviewCards(tasks);
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                start = true;
            }
        }

    }

    //随机漫游 每天一次
    private boolean randomReviewStart = true;
    @Scheduled(cron = "0 0 10 * * ?")
    public void randomReview() throws Exception {
        if (randomReviewStart) {
            randomReviewStart = false;
            try {
                memoryService.randomMemory("ou_5beb69daf448702c6a98adf0a568dfc5");
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                randomReviewStart = true;
            }    }

    }

    //如果有发送提示的卡片12小时没有得到回应, 再次提醒
    @Scheduled(cron = "0 0/1 * * * ?")
    public void memoryReviewAlert() throws Exception {
        if (start) {
            start = false;
            try {
                List<Task> tasks = memoryService.obterTaskAlertList();
                memoryService.sendReviewCards(tasks);
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                start = true;
            }
        }

    }


}
