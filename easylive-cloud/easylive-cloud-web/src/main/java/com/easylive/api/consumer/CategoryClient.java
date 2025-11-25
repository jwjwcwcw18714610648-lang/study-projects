package com.easylive.api.consumer;

import com.easylive.entity.constants.Constants;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@FeignClient(name = Constants.SERVER_NAME_ADMIN)
public interface CategoryClient {
   @RequestMapping("innerApi/loadAllCategory")
    String loadAllCategory();

}
