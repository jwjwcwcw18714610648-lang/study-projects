package com.easylive.entity.vo;

import com.easylive.entity.po.VideoInfo;
import org.elasticsearch.common.recycler.Recycler;

import java.util.List;

public class VideoInfoResultVO {
    private VideoInfo videoInfo;
    public VideoInfoResultVO(VideoInfo videoInfo,List userActionList){
        this.userActionList=userActionList;
        this.videoInfo=videoInfo;
    }
    private List userActionList;
    public VideoInfo getVideoInfo() {
        return videoInfo;
    }

    public void setVideoInfo(VideoInfo videoInfo) {
        this.videoInfo = videoInfo;
    }
}
