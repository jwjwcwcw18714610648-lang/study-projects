package com.easylive.controller;

import com.easylive.api.consumer.WebClient;
import com.easylive.entity.po.UserInfo;
import com.easylive.entity.query.UserInfoQuery;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.entity.vo.ResponseVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/user")
@Validated
public class UserController extends ABaseController {
    @Resource
    private WebClient webClientl;

    @RequestMapping("/loadUser")
    public ResponseVO loadUser(UserInfoQuery userInfoQuery) {
        userInfoQuery.setOrderBy("login_time desc");
        PaginationResultVO resultVO = webClientl.loadUser(userInfoQuery);
        return getSuccessResponseVO(resultVO);
    }

    @RequestMapping("/changeStatus")
    public ResponseVO changeStatus(String userId, Integer status) {
        UserInfo userInfo = new UserInfo();
        userInfo.setStatus(status);
        webClientl.changeStatus(userId, status);
        return getSuccessResponseVO(null);
    }
}
