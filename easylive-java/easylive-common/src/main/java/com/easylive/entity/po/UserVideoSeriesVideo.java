package com.easylive.entity.po;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;


/**
 * 
 */
public class UserVideoSeriesVideo implements Serializable {

	public String getVideoCover() {
		return videoCover;
	}

	public void setVideoCover(String videoCover) {
		this.videoCover = videoCover;
	}

	/**
	 * 列表ID
	 */
	private Integer seriesId;
	private String videoCover;
	private Data createTime;
	private String videoName;
	private Integer playCount;
	/**
	 * 视频ID
	 */
	private String videoId;

	/**
	 * 用户ID
	 */
	private String userId;

	/**
	 * 排序
	 */
	private Integer sort;


	public void setSeriesId(Integer seriesId){
		this.seriesId = seriesId;
	}

	public Integer getSeriesId(){
		return this.seriesId;
	}

	public void setVideoId(String videoId){
		this.videoId = videoId;
	}

	public String getVideoId(){
		return this.videoId;
	}

	public void setUserId(String userId){
		this.userId = userId;
	}

	public String getUserId(){
		return this.userId;
	}

	public void setSort(Integer sort){
		this.sort = sort;
	}

	public Integer getSort(){
		return this.sort;
	}

	@Override
	public String toString (){
		return "列表ID:"+(seriesId == null ? "空" : seriesId)+"，视频ID:"+(videoId == null ? "空" : videoId)+"，用户ID:"+(userId == null ? "空" : userId)+"，排序:"+(sort == null ? "空" : sort);
	}
}
