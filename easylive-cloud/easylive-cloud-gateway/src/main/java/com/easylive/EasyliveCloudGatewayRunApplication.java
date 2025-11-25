package com.easylive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(scanBasePackages = {"com.easylive"})
public class EasyliveCloudGatewayRunApplication {
    public static void main(String[] args) {
        SpringApplication.run(EasyliveCloudGatewayRunApplication.class, args);
    }
}
