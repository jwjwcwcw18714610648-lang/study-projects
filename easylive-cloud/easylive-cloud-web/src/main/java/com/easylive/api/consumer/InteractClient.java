package com.easylive.api.consumer;

import com.easylive.entity.constants.Constants;
import com.easylive.entity.po.UserAction;
import com.easylive.entity.query.UserActionQuery;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = Constants.SERVER_NAME_INTERACT)
public interface InteractClient {

    /**
     * 根据ID删除弹幕
     *
     * @param videoId
     */
    @RequestMapping(Constants.INNER_API_PREFIX + "/danmu/delDanmByVideoId")
    void delDanmuByVideoId(@RequestParam String videoId);

    /**
     * 根据videoId删除评论
     *
     * @param videoId
     */
    @RequestMapping(Constants.INNER_API_PREFIX + "/comment/delCommentByVideoId")
    void delCommentByVideoId(@RequestParam String videoId);


    @RequestMapping(Constants.INNER_API_PREFIX + "/userAction/getUserActionList")
    List<UserAction> getUserActionList(@RequestBody UserActionQuery actionQuery);
}
