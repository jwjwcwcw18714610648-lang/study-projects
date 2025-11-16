package com.easylive.web.controller;

import com.easylive.entity.dto.TokenUserInfoDto;
import com.easylive.entity.enums.ResponseCodeEnum;
import com.easylive.entity.enums.VideoStatusEnum;
import com.easylive.entity.po.VideoInfoFilePost;
import com.easylive.entity.po.VideoInfoPost;
import com.easylive.entity.query.VideoInfoFilePostQuery;
import com.easylive.entity.query.VideoInfoPostQuery;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.entity.vo.VideoPostEditInfoVo;
import com.easylive.entity.vo.VideoStatusCountInfoVO;
import com.easylive.exception.BusinessException;
import com.easylive.service.VideoInfoFilePostService;
import com.easylive.service.VideoInfoPostService;
import com.easylive.service.VideoInfoService;
import com.easylive.utils.JsonUtils;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@RestController
@Validated
@RequestMapping("/ucenter")
public class UCenterVideoPostController extends ABaseController{
    @Resource
    private VideoInfoPostService videoInfoPostService;

    @Resource
    private VideoInfoFilePostService videoInfoFilePostService;

    @Resource
    private VideoInfoService videoInfoService;
    @RequestMapping("/postVideo")

    public ResponseVO postVideo(String videoId, @NotEmpty String videoCover, @NotEmpty @Size(max = 100) String videoName, @NotNull Integer pCategoryId,
                                Integer categoryId, @NotNull Integer postType, @NotEmpty @Size(max = 300) String tags, @Size(max = 2000) String introduction,
                                @Size(max = 3) String interaction, @NotEmpty String uploadFileList) {

        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();//获取token
        List<VideoInfoFilePost> fileInfoList = JsonUtils.convertJsonArray2List(uploadFileList, VideoInfoFilePost.class);//将json转化为列表

        VideoInfoPost videoInfo = new VideoInfoPost();
        videoInfo.setVideoId(videoId);
        videoInfo.setVideoName(videoName);
        videoInfo.setVideoCover(videoCover);
        videoInfo.setpCategoryId(pCategoryId);
        videoInfo.setCategoryId(categoryId);
        videoInfo.setPostType(postType);
        videoInfo.setTags(tags);
        videoInfo.setIntroduction(introduction);
        videoInfo.setInteraction(interaction);

        videoInfo.setUserId(tokenUserInfoDto.getUserId());

       videoInfoPostService.saveVideoInfo(videoInfo, fileInfoList);
        return getSuccessResponseVO(null);
    }
    @RequestMapping("/loadVideoList")

    public ResponseVO loadVideoList(Integer status, Integer pageNo, String videoNameFuzzy) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();  // ① 拿到当前登录用户ID
        VideoInfoPostQuery videoInfoQuery = new VideoInfoPostQuery(); // ② 新建查询对象
        videoInfoQuery.setUserId(tokenUserInfoDto.getUserId());      // ③ 只查"我的"视频
        videoInfoQuery.setOrderBy("v.create_time desc");            // ④ 固定按创建时间倒序
        videoInfoQuery.setPageNo(pageNo);                           // ⑤ 分页页码（空时默认1）

        /* ⑥ 状态筛选核心逻辑 */
        if (status != null) {
            if (status == -1) {   // 前端Tab"审核中"快捷值
                // 排除两个"终态" → 剩下就是"待审/审核中/转码中"
                videoInfoQuery.setExcludeStatusArray(new Integer[]{
                        VideoStatusEnum.STATUS3.getStatus(), // 已通过
                        VideoStatusEnum.STATUS4.getStatus()  // 已拒绝
                });
            } else {
                videoInfoQuery.setStatus(status); // 精准查某个状态码
            }
        }

        videoInfoQuery.setVideoNameFuzzy(videoNameFuzzy); // ⑦ 模糊搜索关键字
        videoInfoQuery.setQueryCountInfo(true);          // ⑧ 让Service顺便返回 totalRows
        PaginationResultVO resultVO = videoInfoPostService.findListByPage(videoInfoQuery); // ⑨ 查库
        return getSuccessResponseVO(resultVO);           // ⑩ 统一包装 {code:200,data:...}
    }
    @RequestMapping ("/getVideoCountInfo")          // 接口路径：/getVideoCountInfo
     // 必须先登录
    public ResponseVO getVideoCountInfo() {
        // 1. 取出当前登录用户
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();

        // 2. 构造查询对象
        VideoInfoPostQuery videoInfoQuery = new VideoInfoPostQuery();
        videoInfoQuery.setUserId(tokenUserInfoDto.getUserId());

        // 3. 审核通过数量
        videoInfoQuery.setStatus(VideoStatusEnum.STATUS3.getStatus());
        Integer auditPassCount = videoInfoPostService.findCountByParam(videoInfoQuery);

        // 4. 审核失败数量
        videoInfoQuery.setStatus(VideoStatusEnum.STATUS4.getStatus());
        Integer auditFailCount = videoInfoPostService.findCountByParam(videoInfoQuery);

        // 5. 正在审核中的数量（排除已通过/已拒绝）
        videoInfoQuery.setStatus(null);
        videoInfoQuery.setExcludeStatusArray(new Integer[]{
                VideoStatusEnum.STATUS3.getStatus(),
                VideoStatusEnum.STATUS4.getStatus()
        });
        Integer inProgress = videoInfoPostService.findCountByParam(videoInfoQuery);

        // 6. 封装返回
        VideoStatusCountInfoVO countInfo = new VideoStatusCountInfoVO();
        countInfo.setAuditPassCount(auditPassCount);
        countInfo.setAuditFailCount(auditFailCount);
        countInfo.setInProgress(inProgress);
        return getSuccessResponseVO(countInfo);
    }
    @RequestMapping("/getVideoByVideoId")

    public ResponseVO getVideoByVideoId(@NotEmpty String videoId) {//获取投稿视频页的视频详情
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        VideoInfoPost videoInfoPost = this.videoInfoPostService.getVideoInfoPostByVideoId(videoId);//获取投稿视频的对象
        if (videoInfoPost == null || !videoInfoPost.getUserId().equals(tokenUserInfoDto.getUserId())) {
            throw new BusinessException(ResponseCodeEnum.CODE_404);//鉴权 视频作者是否与当前用户相同
        }
        VideoInfoFilePostQuery videoInfoFilePostQuery = new VideoInfoFilePostQuery();//查询视频的视频文件发布表
        videoInfoFilePostQuery.setVideoId(videoId);//根据当前的视频id 查名下所有的分p 以及信息
        videoInfoFilePostQuery.setOrderBy("file_index asc");
        List<VideoInfoFilePost> videoInfoFilePostList = this.videoInfoFilePostService.findListByParam(videoInfoFilePostQuery);//将查询到的视频文件对象填入list中
        VideoPostEditInfoVo vo = new VideoPostEditInfoVo();
        vo.setVideoInfo(videoInfoPost);//vo装入视频信息
        vo.setVideoInfoFileList(videoInfoFilePostList);//填入文件信息
        return getSuccessResponseVO(vo);//返回的是视频的基础信息 以及所有分p以及自动填入前端的所有信息
    }
    @RequestMapping("/saveVideoInteraction")

    public ResponseVO saveVideoInteraction(@NotEmpty String videoId, String interaction) {//设置关闭评论区 关闭弹幕的字段
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        videoInfoService.changeInteraction(videoId, tokenUserInfoDto.getUserId(), interaction);
        return getSuccessResponseVO(null);
    }
    @RequestMapping("/deleteVideo")

    public ResponseVO deleteVideo(@NotEmpty String videoId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        videoInfoService.deleteVideo(videoId, tokenUserInfoDto.getUserId());//删除当前视频 视频文件 弹幕 评论
        return getSuccessResponseVO(null);
    }
}
