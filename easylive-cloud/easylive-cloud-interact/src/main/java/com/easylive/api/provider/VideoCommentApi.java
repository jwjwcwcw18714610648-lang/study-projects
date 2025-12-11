package com.easylive.api.provider;

import com.easylive.entity.constants.Constants;
import com.easylive.entity.query.VideoCommentQuery;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.service.VideoCommentService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@RestController
@RequestMapping(Constants.INNER_API_PREFIX + "/comment")
@Validated
public class VideoCommentApi {

    @Resource
    private VideoCommentService videoCommentService;

    @RequestMapping("/delCommentByVideoId")
    public void delCommentByVideoId(@NotEmpty String videoId) {
        VideoCommentQuery videoCommentQuery = new VideoCommentQuery();
        videoCommentQuery.setVideoId(videoId);
        videoCommentService.deleteByParam(videoCommentQuery);
    }

    @RequestMapping("/admin/loadComment")
    public PaginationResultVO loadComment(Integer pageNo, String videoNameFuzzy) {
        VideoCommentQuery commentQuery = new VideoCommentQuery();
        commentQuery.setOrderBy("comment_id desc");
        commentQuery.setPageNo(pageNo);
        commentQuery.setQueryVideoInfo(true);
        commentQuery.setVideoNameFuzzy(videoNameFuzzy);
        PaginationResultVO resultVO = videoCommentService.findListByPage(commentQuery);
        return resultVO;
    }

    @RequestMapping("/admin/delComment")
    public void delComment(@NotNull Integer commentId) {
        videoCommentService.deleteComment(commentId,null);
    }
}
