package com.easylive.web.controller;

import com.easylive.annotation.RecordUserMessage;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.dto.TokenUserInfoDto;
import com.easylive.entity.enums.CommentTopTypeEnum;
import com.easylive.entity.enums.MessageTypeEnum;
import com.easylive.entity.enums.PageSize;
import com.easylive.entity.enums.UserActionTypeEnum;
import com.easylive.entity.po.UserAction;
import com.easylive.entity.po.VideoComment;
import com.easylive.entity.po.VideoInfo;
import com.easylive.entity.query.UserActionQuery;
import com.easylive.entity.query.VideoCommentQuery;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.entity.vo.VideoCommentResultVO;
import com.easylive.service.UserActionService;
import com.easylive.service.VideoCommentService;
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
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Validated
@RequestMapping("/comment")
@Slf4j
public class VideoCommentController extends ABaseController {

    @Resource
    private VideoCommentService videoCommentService;

    @Resource
    private UserActionService userActionService;

    @Resource
    private VideoInfoServiceImpl videoInfoService;

    @RequestMapping("/loadComment")

    public ResponseVO loadComment(@NotEmpty String videoId, Integer pageNo, Integer orderType) {

        VideoInfo videoInfo = videoInfoService.getVideoInfoByVideoId(videoId);
        if (videoInfo.getInteraction() != null && videoInfo.getInteraction().contains("1")) {//如果视频的interaction字段中中包含1 说明作者关闭了评论区
            return getSuccessResponseVO(new ArrayList<>());
        }

        VideoCommentQuery commentQuery = new VideoCommentQuery();
        commentQuery.setVideoId(videoId);
        commentQuery.setPageNo(pageNo);
        commentQuery.setLoadChildren(true);
        commentQuery.setPageSize(PageSize.SIZE15.getSize());
        commentQuery.setpCommentId(0);
        String orderBy = orderType == null || orderType == 0 ? "like_count desc,comment_id desc" : "comment_id desc";//动态热度排序 时间排序
        commentQuery.setOrderBy(orderBy);
        PaginationResultVO<VideoComment> commentData = videoCommentService.findListByPage(commentQuery);//查询所有当前视频的所有评论 并加一排序
        if(pageNo==null||pageNo==1){
            List<VideoComment> topComent=getTopComment(videoId);
            if(!topComent.isEmpty()){
                List<VideoComment> lastComent=
                        commentData.getList().stream().filter(item->!item.getCommentId().equals(topComent.get(0).getCommentId()))
                                .collect(Collectors.toList());//将置顶的评论 在评论区删除
                lastComent.addAll(0,topComent);//再将指定的评论插入到列表第一个
                commentData.setList(lastComent);//重新写入vo
            }
        }

        VideoCommentResultVO resultVO = new VideoCommentResultVO();//规范化
        resultVO.setCommentData(commentData);
        List<UserAction> userActionList = new ArrayList<>();
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();//拿到当前的登陆者信息
        if (tokenUserInfoDto != null) {
            UserActionQuery userActionQuery = new UserActionQuery();
            userActionQuery.setUserId(tokenUserInfoDto.getUserId());
            userActionQuery.setVideoId(videoId);
            userActionQuery.setActionTypeArray(new Integer[]{UserActionTypeEnum.COMMENT_LIKE.getType(), UserActionTypeEnum.COMMENT_HATE.getType()});
            userActionList = userActionService.findListByParam(userActionQuery);//拿到此人的行为
        }
        resultVO.setUserActionList(userActionList);
        return getSuccessResponseVO(resultVO);
    }
    private List<VideoComment> getTopComment(String videoId){
        VideoCommentQuery videoCommentQuery=new VideoCommentQuery();
        videoCommentQuery.setVideoId(videoId);
        videoCommentQuery.setLoadChildren(true);
        videoCommentQuery.setTopType(CommentTopTypeEnum.TOP.getType());
        return this.videoCommentService.findListByParam(videoCommentQuery);
    }
    @RequestMapping("/postComment")//添加评论 且已知评论id是自增的
    @RecordUserMessage(messageType = MessageTypeEnum.COMMENT)
    public ResponseVO postComment(@NotEmpty String videoId,
                                  Integer replyCommentId,
                                  @NotEmpty @Size(max = 500) String content,
                                  @Size(max = 50) String imgPath) {

        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        VideoComment comment = new VideoComment();
        comment.setUserId(tokenUserInfoDto.getUserId());
        comment.setAvatar(tokenUserInfoDto.getAvatar());//头像
        comment.setNickName(tokenUserInfoDto.getNickName());
        comment.setVideoId(videoId);//视频id
        comment.setContent(content);//内容
        comment.setImgPath(imgPath);//图片
        videoCommentService.postComment(comment, replyCommentId);
        return getSuccessResponseVO(comment);
    }
    @RequestMapping("/topComment")

    public ResponseVO topComment(@NotNull Integer commentId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        videoCommentService.topComment(commentId, tokenUserInfoDto.getUserId());
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/cancelTopComment")

    public ResponseVO cancelTopComment(@NotNull Integer commentId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        videoCommentService.cancelTopComment(commentId, tokenUserInfoDto.getUserId());
        return getSuccessResponseVO(null);
    }
    @RequestMapping("/userDelComment")

    public ResponseVO userDelComment(@NotNull Integer commentId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
        VideoComment comment = new VideoComment();
        videoCommentService.deleteComment(commentId, tokenUserInfoDto.getUserId());
        return getSuccessResponseVO(comment);
    }

}