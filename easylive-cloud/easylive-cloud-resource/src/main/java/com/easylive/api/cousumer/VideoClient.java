package com.easylive.api.cousumer;

import com.easylive.entity.constants.Constants;
import com.easylive.entity.po.VideoInfoFile;
import com.easylive.entity.po.VideoInfoFilePost;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = Constants.SERVER_NAME_WEB,contextId = "webClientA")
public interface VideoClient {
    @RequestMapping(Constants.INNER_API_PREFIX+"/video/getVideoInfoFileByFileId")
    public VideoInfoFile getVideoInfoFileByFileId(@RequestParam String fileId);
    @PostMapping(Constants.INNER_API_PREFIX + "/video/transferVideoFile4Db")
    VideoInfoFile transferVideoFile4Db(@RequestParam String videoId,
                                       @RequestParam String uploadId,
                                       @RequestParam String userId,
                                       @RequestBody VideoInfoFilePost updateFilePost);
}
