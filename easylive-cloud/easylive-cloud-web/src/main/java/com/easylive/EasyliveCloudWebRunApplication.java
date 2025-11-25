package com.easylive;



import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;


@SpringBootApplication(scanBasePackages = "com.easylive")
@EnableFeignClients
public class EasyliveCloudWebRunApplication {
    public static void main(String[] args) {
        SpringApplication.run(EasyliveCloudWebRunApplication.class, args);
    }
}
