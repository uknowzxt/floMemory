package com.uknowz;


import com.uknowz.Service.ThirdLib.FeishuLib;
import com.uknowz.Service.ThirdLib.ImSample;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ApplicationSsm.class)//扫描启动类
public class UserControllerTest {

    @Autowired
    FeishuLib feishuLib;

    @Test
    public void sendCard() throws Exception {
        String s = ImSample.sendInteractiveMonitorMsg(feishuLib.getClient(), "ou_5beb69daf448702c6a98adf0a568dfc5","img_v2_38da9642-a1bd-4ee6-97c7-bdb9950e094g","自尊","看看自尊的内容和成就动机的区别","");

//        ImSample.sendInteractiveMonitorMsg(feishuLib.getClient(),"ou_5beb69daf448702c6a98adf0a568dfc5");
    }


}