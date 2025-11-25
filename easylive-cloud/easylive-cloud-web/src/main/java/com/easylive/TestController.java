package com.easylive;

import com.easylive.api.consumer.CategoryClient;
import org.springframework.beans.CachedIntrospectionResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
public class TestController {
    @Resource
    private CategoryClient categoryClient;
    @RequestMapping("/test")
    public String test(){
        return categoryClient.loadAllCategory();
    }
}
