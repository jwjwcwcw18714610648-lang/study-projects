package com.easylive.api.provider;

import com.easylive.entity.constants.Constants;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/innerApi")
public class CategoryApi {


    @RequestMapping("/loadAllCategory")
    public String loadAllCategory() {

        return "这里是admin提供的分类方法";
    }
}
