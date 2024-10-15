package com.kary.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class Application {

    public static void main(String[] args) {
        System.setProperty("http.proxyHost","127.0.0.1");
        System.setProperty("http.proxyPort","7890"); // 修改为你代理软件的端口
        System.setProperty("https.proxyHost","127.0.0.1");
        System.setProperty("https.proxyPort","7890"); // 同理
        SpringApplication.run(Application.class, args);
    }

}
