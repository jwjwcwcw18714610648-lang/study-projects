package com.easylive.web.controller;

import com.easylive.entity.constants.Constants;
import com.easylive.entity.po.VideoDanmu;
import com.easylive.entity.po.VideoInfo;
import com.easylive.entity.query.VideoDanmuQuery;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.service.VideoDanmuService;
import com.easylive.service.impl.VideoInfoServiceImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Date;

@RestController
@Validated
@RequestMapping("/danmu")
@Slf4j
public class VideoDanmuController extends ABaseController {

    @Resource
    private VideoDanmuService videoDanmuService;

    @Resource
    private VideoInfoServiceImpl videoInfoService;

    @RequestMapping("/loadDanmu")//搜索弹幕表中所有数据

    public ResponseVO loadDanmu(@NotEmpty String fileId, @NotEmpty String videoId) {

        VideoInfo videoInfo = videoInfoService.getVideoInfoByVideoId(videoId);
        if (videoInfo.getInteraction() != null && videoInfo.getInteraction().contains(Constants.ZERO.toString())) {
            return getSuccessResponseVO(new ArrayList<>());
        }


        VideoDanmuQuery videoDanmuQuery = new VideoDanmuQuery();
        videoDanmuQuery.setFileId(fileId);
        videoDanmuQuery.setOrderBy("danmu_id asc");
        return getSuccessResponseVO(videoDanmuService.findListByParam(videoDanmuQuery));
    }

    @RequestMapping("/postDanmu")//发送弹幕

    public ResponseVO postDanmu(@NotEmpty String videoId,
                                @NotEmpty String fileId,//分p ID
                                @NotEmpty @Size(max = 200) String text,
                                @NotNull Integer mode,
                                @NotEmpty String color,
                                @NotNull Integer time) {
        VideoDanmu videoDanmu = new VideoDanmu();//传入对象
        videoDanmu.setVideoId(videoId);
        videoDanmu.setFileId(fileId);
        videoDanmu.setText(text);
        videoDanmu.setMode(mode);
        videoDanmu.setColor(color);
        videoDanmu.setTime(time);
        videoDanmu.setUserId(getTokenUserInfoDto().getUserId());
        videoDanmu.setPostTime(new Date());
        videoDanmuService.saveVideoDanmu(videoDanmu);//数据库层面
        return getSuccessResponseVO(null);
    }
}
