package com.easylive.web;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(scanBasePackages = {"com.easylive"})
@MapperScan(basePackages = {"com.easylive.mappers"})
@EnableTransactionManagement
@EnableScheduling
public class EasyliveWebApplication {
    public static void main(String[] args) {
        SpringApplication.run(EasyliveWebApplication.class,args);
    }
}
/*
@SpringBootApplication：

组合注解：@Configuration + @EnableAutoConfiguration + @ComponentScan。

scanBasePackages = {"com.easylive"}：指定扫描 com.easylive 包及其子包中的 Spring 组件。

exclude = {DataSourceAutoConfiguration.class}：排除自动配置中的 DataSourceAutoConfiguration（不自动配置数据源）。

main 方法：

启动 Spring Boot 应用：SpringApplication.run()。

传入当前类 EasyliveWebApplication 和命令行参数 args 启动应用。*/
