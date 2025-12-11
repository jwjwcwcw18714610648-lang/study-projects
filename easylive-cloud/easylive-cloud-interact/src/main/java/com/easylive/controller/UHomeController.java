package com.easylive.controller;

import com.easylive.entity.constants.Constants;
import com.easylive.entity.dto.TokenUserInfoDto;
import com.easylive.entity.enums.PageSize;
import com.easylive.entity.enums.UserActionTypeEnum;
import com.easylive.entity.enums.VideoOrderTypeEnum;
import com.easylive.entity.po.UserInfo;

import com.easylive.entity.query.UserActionQuery;
import com.easylive.entity.query.VideoInfoQuery;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.entity.vo.ResponseVO;

import com.easylive.service.UserActionService;
import com.easylive.utils.CopyTools;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@RestController
@Validated
@RequestMapping("/uhome")
public class UHomeController extends ABaseController {

@Resource
private UserActionService userActionService;



    @RequestMapping("/loadUserCollection")

    public ResponseVO loadUserCollection(@NotEmpty String userId, Integer pageNo) {//传入被观测者的userid
        UserActionQuery actionQuery = new UserActionQuery();
        actionQuery.setActionType(UserActionTypeEnum.VIDEO_COLLECT.getType());//需要查询当前用户收藏的视频
        actionQuery.setUserId(userId);
        actionQuery.setPageNo(pageNo);
        actionQuery.setQueryVideoInfo(true);//需要设置需要关联查询视频详细信息
        actionQuery.setOrderBy("action_time desc");
        PaginationResultVO resultVO = userActionService.findListByPage(actionQuery);
        return getSuccessResponseVO(resultVO);
    }
}
