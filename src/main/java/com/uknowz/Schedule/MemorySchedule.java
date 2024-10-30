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

    private static String[] candidates = {"张晓婷","张阅"};
    private static LocalDate startDate = LocalDate.of(2023, 11, 27); // Replace with your own start date


    public static void main(String[] args) throws Exception {
        helpForJob();
    }
    //工作辅助
    //每天九点半推送今天值班
//    @Scheduled(cron = "0 0/1 * * * ?")
    @Scheduled(cron = "0 28 12 * * ?")
    public static void helpForJob() throws Exception {


        LocalDate currentDate = LocalDate.now();
        DayOfWeek currentDayOfWeek = currentDate.getDayOfWeek();
        if (currentDayOfWeek == DayOfWeek.SATURDAY || currentDayOfWeek == DayOfWeek.SUNDAY) {
            System.out.println("It's the weekend, no one is on duty.");
//            return;
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
        System.out.println(String.format("今日广告对接技术值班:%s",personOnDuty));
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
    boolean memoryReviewAlertStart = true;
    @Scheduled(cron = "0 0/1 * * * ?")
    public void memoryReviewAlert() throws Exception {
        if (memoryReviewAlertStart) {
            memoryReviewAlertStart = false;
            try {
                List<Task> tasks = memoryService.obterTaskAlertList();
                memoryService.sendReviewCards(tasks);
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                memoryReviewAlertStart = true;
            }
        }

    }


    //发送当天提醒
    boolean cueDoListStart = true;
    @Scheduled(cron = "0 0/1 * * * ?")
    public void cueDoList() throws Exception {
        if (cueDoListStart) {
            cueDoListStart = false;
            try {
                memoryService.sendCueMeDOList("ou_5beb69daf448702c6a98adf0a568dfc5");
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                cueDoListStart = true;
            }
        }

    }



}
