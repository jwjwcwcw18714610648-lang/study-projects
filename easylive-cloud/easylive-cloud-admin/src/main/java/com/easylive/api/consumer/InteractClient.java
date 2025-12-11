package com.easylive.api.consumer;

import com.easylive.entity.constants.Constants;
import com.easylive.entity.vo.PaginationResultVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = Constants.SERVER_NAME_INTERACT)
public interface InteractClient {

    @RequestMapping(Constants.INNER_API_PREFIX + "/danmu/admin/loadDanmu")
    PaginationResultVO loadDanmu(@RequestParam(required = false) Integer pageNo, @RequestParam(required = false) String videoNameFuzzy);

    @RequestMapping(Constants.INNER_API_PREFIX + "/danmu/admin/delDanmu")
    void delDanmu(@RequestParam Integer danmuId);

    @RequestMapping(Constants.INNER_API_PREFIX + "/comment/admin/loadComment")
    PaginationResultVO loadComment(@RequestParam(required = false) Integer pageNo, @RequestParam(required = false) String videoNameFuzzy);

    @RequestMapping(Constants.INNER_API_PREFIX + "/comment/admin/delComment")
    void delComment(@RequestParam Integer commentId);

    @RequestMapping(Constants.INNER_API_PREFIX + "/message/admin/saveUserMessage")
    void saveUserMessage(@RequestParam String videoId, @RequestParam String content);
}
