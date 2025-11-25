package com.easylive;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.easylive"})
@MapperScan(basePackages = {"com.easylive.mappers"})
@EnableFeignClients
public class EasyliveCloudAdminRunApplication {
    public static void main(String[] args) {
        SpringApplication.run(EasyliveCloudAdminRunApplication.class, args);
    }
}
