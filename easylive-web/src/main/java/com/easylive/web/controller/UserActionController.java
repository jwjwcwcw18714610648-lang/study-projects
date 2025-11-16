package com.easylive.web.controller;

import com.easylive.annotation.RecordUserMessage;
import com.easylive.entity.enums.MessageTypeEnum;
import com.easylive.entity.po.UserAction;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.service.UserActionService;

import com.easylive.web.annotation.GlobalInterceptor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;

/**
 * 用户行为 点赞、评论 Controller
 */
@RestController("userActionController")
@RequestMapping("/userAction")
public class UserActionController extends ABaseController {

    @Resource
    private UserActionService userActionService;

    @RequestMapping("doAction")
    @GlobalInterceptor(checkLogin = true)
    @RecordUserMessage(messageType = MessageTypeEnum.LIKE)
    public ResponseVO doAction(@NotEmpty String videoId,
                               @NotEmpty Integer actionType,//操作类型 点赞收藏以及投币
                               @Max(2) @Min(1) Integer actionCount,
                               Integer commentId) {//评论id
        UserAction userAction = new UserAction();
        userAction.setUserId(getTokenUserInfoDto().getUserId());
        userAction.setVideoId(videoId);
        userAction.setActionType(actionType);
        actionCount = actionCount == null ? 1 : actionCount;//没传默认为1 否则就传多少是多少
        userAction.setActionCount(actionCount);
        commentId = commentId == null ? 0 : commentId;//没传评论就表示是视频的
        userAction.setCommentId(commentId);
        userActionService.saveAction(userAction);
        return getSuccessResponseVO(null);
    }

}