package com.easylive.controller;

import com.easylive.api.consumer.WebClient;
import com.easylive.entity.enums.StatisticsTypeEnum;
import com.easylive.entity.po.StatisticsInfo;
import com.easylive.entity.query.UserInfoQuery;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.utils.DateUtil;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/index")
@Validated
public class IndexController extends ABaseController {

    @Resource
    private WebClient webClient;
    @RequestMapping("/getActualTimeStatisticsInfo")
    public ResponseVO getActualTimeStatisticsInfo() {

        return getSuccessResponseVO(webClient.getActualTimeStatisticsInfo());
    }

    @RequestMapping("/getWeekStatisticsInfo")
    public ResponseVO getWeekStatisticsInfo(Integer dataType) {

        return getSuccessResponseVO(webClient.getWeekStatisticsInfo(dataType));
    }
}
