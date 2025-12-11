package com.easylive.controller;

import com.easylive.entity.dto.TokenUserInfoDto;
import com.easylive.entity.po.VideoInfo;

import com.easylive.entity.query.VideoInfoQuery;
import com.easylive.entity.vo.ResponseVO;

import com.easylive.service.VideoInfoService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.annotation.Resource;
import java.util.List;

@RestController
@Validated
@RequestMapping("/ucenter")
public class UCenterInteractController extends ABaseController {



    @Resource
    private VideoInfoService videoInfoService;

    @RequestMapping("/loadAllVideo")

    public ResponseVO loadAllVideo() {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        VideoInfoQuery videoInfoQuery = new VideoInfoQuery();
        videoInfoQuery.setUserId(tokenUserInfoDto.getUserId());
        videoInfoQuery.setOrderBy("create_time desc");
        List<VideoInfo> videoInfoList = videoInfoService.findListByParam(videoInfoQuery);//查出当前用户的所有视频
        return getSuccessResponseVO(videoInfoList);
    }

}
