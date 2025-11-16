package com.easylive.web.controller;

import com.easylive.entity.dto.TokenUserInfoDto;
import com.easylive.entity.enums.ResponseCodeEnum;
import com.easylive.entity.po.UserVideoSeries;
import com.easylive.entity.po.UserVideoSeriesVideo;
import com.easylive.entity.po.VideoInfo;
import com.easylive.entity.query.UserVideoSeriesQuery;
import com.easylive.entity.query.UserVideoSeriesVideoQuery;
import com.easylive.entity.query.VideoInfoQuery;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.entity.vo.UserVideoSeriesDetailVO;
import com.easylive.exception.BusinessException;
import com.easylive.service.UserVideoSeriesService;
import com.easylive.service.UserVideoSeriesVideoService;
import com.easylive.service.VideoInfoService;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Validated
@RequestMapping("/uhome/series")
public class UHomeVideoSeriesController extends ABaseController {

    @Resource
    private VideoInfoService videoInfoService;

    @Resource
    private UserVideoSeriesService userVideoSeriesService;

    @Resource
    private UserVideoSeriesVideoService userVideoSeriesVideoService;

    @RequestMapping("/loadVideoSeries")

    public ResponseVO loadVideoSeries(@NotEmpty String userId) {
        List<UserVideoSeries> videoSeries = userVideoSeriesService.getUserAllSeries(userId);
        return getSuccessResponseVO(videoSeries);
    }

    /**
     * 保存系列
     *
     * @param seriesId
     * @param seriesName
     * @param seriesDescription
     * @param videoIds
     * @return
     */
    @RequestMapping("/saveVideoSeries")

   public ResponseVO saveVideoSeries(Integer seriesId,
                                      @NotEmpty @Size(max = 100) String seriesName,//分类id 不传则为新增 传则说明修改    分类名称 分类介绍  传过来的视频id集合
                                      @Size(max = 200) String seriesDescription,
                                      String videoIds) {
       TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
       UserVideoSeries videoSeries = new UserVideoSeries();
        videoSeries.setUserId(tokenUserInfoDto.getUserId());
        videoSeries.setSeriesId(seriesId);
      videoSeries.setSeriesName(seriesName);
       videoSeries.setSeriesDescription(seriesDescription);
       userVideoSeriesService.saveUserVideoSeries(videoSeries, videoIds);
        return getSuccessResponseVO(null);
    }
    @RequestMapping("/loadAllVideo")

    public ResponseVO loadAllVideo(Integer seriesId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        VideoInfoQuery infoQuery = new VideoInfoQuery();
        if (seriesId != null) {
            UserVideoSeriesVideoQuery videoSeriesVideoQuery = new UserVideoSeriesVideoQuery();
            videoSeriesVideoQuery.setSeriesId(seriesId);
            videoSeriesVideoQuery.setUserId(tokenUserInfoDto.getUserId());
            List<UserVideoSeriesVideo> seriesVideoList = userVideoSeriesVideoService.findListByParam(videoSeriesVideoQuery);
            List<String> videoList = seriesVideoList.stream().map(item -> item.getVideoId()).collect(Collectors.toList());
            infoQuery.setExcludeVideoIdArray(videoList.toArray(new String[videoList.size()]));//查询分类下的视频的所有id 传给videoinfoquery
        }
        infoQuery.setUserId(tokenUserInfoDto.getUserId());
        List<VideoInfo> videoInfoList = videoInfoService.findListByParam(infoQuery);
        return getSuccessResponseVO(videoInfoList);
    }
    @RequestMapping("/getVideoSeriesDetail")
    public ResponseVO getVideoSeriesDetail(@NotNull Integer seriesId){
        //传入分类id 获取id相关的所有信息 包括视频
        UserVideoSeries userVideoSeries=userVideoSeriesService.getUserVideoSeriesBySeriesId(seriesId);
        if(userVideoSeries==null){
            throw new BusinessException(ResponseCodeEnum.CODE_404);
        }
        UserVideoSeriesVideoQuery userVideoSeriesVideoQuery=new UserVideoSeriesVideoQuery();
        userVideoSeriesVideoQuery.setSeriesId(seriesId);
        userVideoSeriesVideoQuery.setOrderBy("sort asc");
        userVideoSeriesVideoQuery.setQueryVideoInfo(true);//是否关联视频表查询
        List<UserVideoSeriesVideo> seriesVideoList=userVideoSeriesVideoService.findListByParam(userVideoSeriesVideoQuery);
        return getSuccessResponseVO(new UserVideoSeriesDetailVO(userVideoSeries,seriesVideoList));
    }
    @RequestMapping("/saveSeriesVideo")

    public ResponseVO saveSeriesVideo(@NotNull Integer seriesId, @NotEmpty String videoIds) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        userVideoSeriesService.saveSeriesVideo(tokenUserInfoDto.getUserId(), seriesId, videoIds);
        return getSuccessResponseVO(null);
    }
    @RequestMapping("/delVideoSeries")

    public ResponseVO delVideoSeries(@NotNull Integer seriesId) {//删除整个分类集合
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        userVideoSeriesService.delVideoSeries(tokenUserInfoDto.getUserId(), seriesId);
        return getSuccessResponseVO(null);
    }
    @RequestMapping("/delSeriesVideo")

    public ResponseVO delSeriesVideo(@NotNull Integer seriesId, @NotEmpty String videoId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();//删除分类集合中的一个视频
        userVideoSeriesService.delSeriesVideo(tokenUserInfoDto.getUserId(), seriesId, videoId);
        return getSuccessResponseVO(null);
    }
    /**
     * 系列排序
     *
     * @param seriesIds
     * @return
     */
    @RequestMapping("/changeVideoSeriesSort")

    public ResponseVO changeVideoSeriesSort(@NotEmpty String seriesIds) {//实现分类的拖动排序 传入分类之后的id集合
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        userVideoSeriesService.changeVideoSeriesSort(tokenUserInfoDto.getUserId(), seriesIds);
        return getSuccessResponseVO(null);
    }
    @RequestMapping("/loadVideoSeriesWithVideo")

    public ResponseVO loadVideoSeriesWithVideo(@NotEmpty String userId) {//鉴权
        UserVideoSeriesQuery seriesQuery = new UserVideoSeriesQuery();
        seriesQuery.setUserId(userId);
        seriesQuery.setOrderBy("sort asc");
        List<UserVideoSeries> videoSeries = userVideoSeriesService.findListWithVideoList(seriesQuery);
        return getSuccessResponseVO(videoSeries);
    }
}
