package com.uknowz.Service.ThirdLib;

import com.lark.oapi.Client;
import com.lark.oapi.core.enums.BaseUrlEnum;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class FeishuLib {
    private static String appId = "cli_a4cc93c729b09013";
    private static String appSecret = "lp9JlA5eoyOTTdaWiIPkQc6rlwis0sam";

    public static Client client = Client.newBuilder(appId,appSecret)
//            .marketplaceApp() // 设置 app 类型为商店应用
            .openBaseUrl(BaseUrlEnum.FeiShu) // 设置域名，默认为飞书
//                .helpDeskCredential("helpDeskId","helpDeskSecret") // 服务台应用才需要设置
            .requestTimeout(3, TimeUnit.SECONDS) // 设置httpclient 超时时间，默认永不超时
//                .disableTokenCache() // 禁用token管理，禁用后需要开发者自己传递token
            .logReqAtDebug(true) // 在 debug 模式下会打印 http 请求和响应的 headers,body 等信息。
            .build();
//        memoryService.


    public static Client getClient() {
        return client;
    }
}
