package com.easylive.api.provider;

import com.easylive.annotation.RecordUserMessage;
import com.easylive.component.EsSearchComponent;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.enums.MessageTypeEnum;
import com.easylive.entity.enums.SearchOrderTypeEnum;
import com.easylive.entity.po.VideoInfo;
import com.easylive.entity.po.VideoInfoFile;
import com.easylive.entity.po.VideoInfoFilePost;
import com.easylive.entity.po.VideoInfoPost;
import com.easylive.entity.query.VideoInfoFilePostQuery;
import com.easylive.entity.query.VideoInfoPostQuery;
import com.easylive.entity.query.VideoInfoQuery;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.service.VideoInfoFilePostService;
import com.easylive.service.VideoInfoFileService;
import com.easylive.service.VideoInfoPostService;
import com.easylive.service.VideoInfoService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping(Constants.INNER_API_PREFIX + "/video")
@Validated
public class VideoInfoApi {

    @Resource
    private VideoInfoService videoInfoService;

    @Resource
    private VideoInfoPostService videoInfoPostService;

    @Resource
    private VideoInfoFileService videoInfoFileService;

    @Resource
    private VideoInfoFilePostService videoInfoFilePostService;

    @Resource
    private EsSearchComponent esSearchComponent;

    @RequestMapping("/getVideoInfoByVideoId")
    public VideoInfo getVideoInfo(@NotEmpty String videoId) {
        return videoInfoService.getVideoInfoByVideoId(videoId);
    }

    @RequestMapping("/getVideoInfoPostByVideoId")
    public VideoInfoPost getVideoInfoPost(@NotEmpty String videoId) {
        return videoInfoPostService.getVideoInfoPostByVideoId(videoId);
    }

    @RequestMapping("/updateCountInfo")
    public void updateCountInfo(@NotEmpty String videoId, @NotEmpty String field, @NotNull Integer changeCount) {
        videoInfoService.updateCountInfo(videoId, field, changeCount);
    }

    @RequestMapping("/getVideoCount")
    public Integer getVideoCount(@RequestBody VideoInfoQuery videoInfoQuery) {
        return videoInfoService.findCountByParam(videoInfoQuery);
    }

    @RequestMapping("/getVideoInfoFileByFileId")
    public VideoInfoFile getVideoInfoFileByFileId(@NotEmpty String fileId) {
        VideoInfoFile videoInfoFile = videoInfoFileService.getVideoInfoFileByFileId(fileId);
        return videoInfoFile;
    }

    @RequestMapping("/transferVideoFile4Db")
    public void transferVideoFile4Db(@RequestParam String videoId, @RequestParam String uploadId, @RequestParam String userId,
                                     @RequestBody VideoInfoFilePost updateFilePost) {
        videoInfoPostService.transferVideoFile4Db(videoId, uploadId, userId, updateFilePost);
    }

    @RequestMapping("/updateDocCount")
    public void updateDocCount(String videoId, SearchOrderTypeEnum searchOrderTypeEnum, Integer changeCOunt) {
        esSearchComponent.updateDocCount(videoId, searchOrderTypeEnum.getField(), changeCOunt);
    }

    @RequestMapping("/admin/loadVideoList")
    public PaginationResultVO loadVideoList(@RequestBody VideoInfoPostQuery videoInfoPostQuery) {
        videoInfoPostQuery.setOrderBy("v.last_update_time desc");
        videoInfoPostQuery.setQueryCountInfo(true);
        videoInfoPostQuery.setQueryUserInfo(true);
        PaginationResultVO resultVO = videoInfoPostService.findListByPage(videoInfoPostQuery);
        return resultVO;
    }

    @RequestMapping("/admin/recommendVideo")
    public void recommendVideo(@NotEmpty String videoId) {
        videoInfoPostService.recommendVideo(videoId);
    }

    @RequestMapping("/admin/auditVideo")
    @RecordUserMessage(messageType = MessageTypeEnum.SYS)
    public void auditVideo(@NotEmpty String videoId, @NotNull Integer status, String reason) throws IOException {
        videoInfoPostService.auditVideo(videoId, status, reason);
    }

    @RequestMapping("/admin/deleteVideo")
    public void deleteVideo(@NotEmpty String videoId) {
        videoInfoService.deleteVideo(videoId, null);
    }

    @RequestMapping("/admin/loadVideoPList")
    public List<VideoInfoFilePost> loadVideoPList(@NotEmpty String videoId) {
        VideoInfoFilePostQuery postQuery = new VideoInfoFilePostQuery();
        postQuery.setOrderBy("file_index asc");
        postQuery.setVideoId(videoId);
        List<VideoInfoFilePost> videoInfoFilePostsList = videoInfoFilePostService.findListByParam(postQuery);
        return videoInfoFilePostsList;
    }


}
