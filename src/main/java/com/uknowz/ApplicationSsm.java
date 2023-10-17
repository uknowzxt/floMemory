package com.uknowz;

import com.lark.oapi.sdk.servlet.ext.ServletAdapter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @Author: tengyun
 * @Date:Create in  2018/8/9 下午8:40
 * @description: https://www.jianshu.com/p/bc4700815e08
 */

@EnableScheduling
@ServletComponentScan
@ComponentScan(basePackages = {"com.uknowz"})
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class ApplicationSsm extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(ApplicationSsm.class);
    }

    // 注入扩展实例到 IOC 容器
    @Bean
    public ServletAdapter getServletAdapter() {
        return new ServletAdapter();
    }

    public static void main(String[] args) {
        SpringApplication.run(ApplicationSsm.class, args);
    }
}