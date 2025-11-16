package com.easylive.service.impl;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Resource;

import com.easylive.component.EsSearchComponent;
import com.easylive.component.RedisComponent;
import com.easylive.entity.config.AppConfig;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.dto.SysSettingDto;
import com.easylive.entity.enums.ResponseCodeEnum;
import com.easylive.entity.enums.UserActionTypeEnum;
import com.easylive.entity.po.*;
import com.easylive.entity.query.*;
import com.easylive.exception.BusinessException;
import com.easylive.mappers.*;
import com.easylive.service.UserInfoService;
import com.easylive.service.VideoInfoPostService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.easylive.entity.enums.PageSize;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.service.VideoInfoService;
import com.easylive.utils.StringTools;
import org.springframework.transaction.annotation.Transactional;


/**
 * 视频信息 业务接口实现
 */
@Service("videoInfoService")
@Slf4j
public class VideoInfoServiceImpl implements VideoInfoService {
	@Resource
	private EsSearchComponent esSearchComponent;
	private static ExecutorService executorService = Executors.newFixedThreadPool(10);//十个线程的线程池
	@Resource
	private VideoInfoPostMapper videoInfoPostMapper;
	@Resource
	private VideoInfoMapper<VideoInfo, VideoInfoQuery> videoInfoMapper;
	@Resource
	private VideoInfoFileMapper<VideoInfoFile, VideoInfoFileQuery> videoInfoFileMapper;
	@Resource
	private VideoInfoFilePostMapper<VideoInfoFilePost, VideoInfoFilePostQuery> videoInfoFilePostMapper;
	@Resource
	private VideoDanmuMapper<VideoDanmu, VideoDanmuQuery> videoDanmuMapper;
	@Resource
	private RedisComponent redisComponent;
	@Resource
	private UserInfoService userInfoService;
	@Resource
	private VideoCommentMapper<VideoComment, VideoCommentQuery> videoCommentMapper;
	@Resource
	private AppConfig appConfig;
    @Autowired
    private UserInfoMapper userInfoMapper;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<VideoInfo> findListByParam(VideoInfoQuery param) {
		return this.videoInfoMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(VideoInfoQuery param) {
		return this.videoInfoMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<VideoInfo> findListByPage(VideoInfoQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<VideoInfo> list = this.findListByParam(param);
		PaginationResultVO<VideoInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(VideoInfo bean) {
		return this.videoInfoMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<VideoInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.videoInfoMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<VideoInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.videoInfoMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(VideoInfo bean, VideoInfoQuery param) {
		StringTools.checkParam(param);
		return this.videoInfoMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(VideoInfoQuery param) {
		StringTools.checkParam(param);
		return this.videoInfoMapper.deleteByParam(param);
	}

	/**
	 * 根据VideoId获取对象
	 */
	@Override
	public VideoInfo getVideoInfoByVideoId(String videoId) {
		return this.videoInfoMapper.selectByVideoId(videoId);
	}

	/**
	 * 根据VideoId修改
	 */
	@Override
	public Integer updateVideoInfoByVideoId(VideoInfo bean, String videoId) {
		return this.videoInfoMapper.updateByVideoId(bean, videoId);
	}

	/**
	 * 根据VideoId删除
	 */
	@Override
	public Integer deleteVideoInfoByVideoId(String videoId) {
		return this.videoInfoMapper.deleteByVideoId(videoId);
	}
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void changeInteraction(String videoId, String userId, String interaction) {
		VideoInfo videoInfo = new VideoInfo();//更改的是已经发布的表 并不用videoinfopost
		videoInfo.setInteraction(interaction);
		VideoInfoQuery videoInfoQuery = new VideoInfoQuery();
		videoInfoQuery.setVideoId(videoId);
		videoInfoQuery.setUserId(userId);
		videoInfoMapper.updateByParam(videoInfo, videoInfoQuery);//更新操作即可


		VideoInfoPost videoInfoPost = new VideoInfoPost();//也得改发布的表中的信息 因为也有这个字段
		videoInfoPost.setInteraction(interaction);
		VideoInfoPostQuery videoInfoPostQuery = new VideoInfoPostQuery();
		videoInfoPostQuery.setVideoId(videoId);
		videoInfoPostQuery.setUserId(userId);
		videoInfoPostMapper.updateByParam(videoInfoPost, videoInfoPostQuery);
	}

	@Override
	public void addReadCount(String videoId) {
		this.videoInfoMapper.updateCountInfo(videoId, UserActionTypeEnum.VIDEO_PLAY.getField(), 1);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteVideo(String videoId, String userId) {
		VideoInfoPost videoInfoPost = (VideoInfoPost) this.videoInfoPostMapper.selectByVideoId(videoId);//根据视频id 查到发布表的视频
		if (videoInfoPost == null || userId != null && !userId.equals(videoInfoPost.getUserId())) {
			throw new BusinessException(ResponseCodeEnum.CODE_404);
		}

		this.videoInfoMapper.deleteByVideoId(videoId);

		this.videoInfoPostMapper.deleteByVideoId(videoId);
		//在视频表和视频发布表中删除 因为这俩都是一条信息的 太过于好删了

		/**
		 * 删除用户硬币
		 */
		SysSettingDto sysSettingDto = redisComponent.getSysSettingDto();
		userInfoMapper.updateCoinCountInfo(videoInfoPost.getUserId(), -sysSettingDto.getPostVideoCoinCount());
		/**
		 * 删除es信息
		 */
		esSearchComponent.delDoc(videoId);
	//线程池异步删除 为什么异步？ 视频删除涉及文件IO（删硬盘），速度很慢。如果同步执行，用户要卡住等好几秒。异步就是让服务员慢慢删，主线程立刻返回“删除成功”
		executorService.execute(() -> {

			VideoInfoFileQuery videoInfoFileQuery = new VideoInfoFileQuery();
			videoInfoFileQuery.setVideoId(videoId);
			//查询分P
			List<VideoInfoFile> videoInfoFileList = this.videoInfoFileMapper.selectList(videoInfoFileQuery);

			//删除分P
			videoInfoFileMapper.deleteByParam(videoInfoFileQuery);

			VideoInfoFilePostQuery videoInfoFilePostQuery = new VideoInfoFilePostQuery();
			videoInfoFilePostQuery.setVideoId(videoId);
			videoInfoFilePostMapper.deleteByParam(videoInfoFilePostQuery);

			//删除弹幕
			VideoDanmuQuery videoDanmuQuery = new VideoDanmuQuery();
			videoDanmuQuery.setVideoId(videoId);
			videoDanmuMapper.deleteByParam(videoDanmuQuery);

			//删除评论
			VideoCommentQuery videoCommentQuery = new VideoCommentQuery();
			videoCommentQuery.setVideoId(videoId);
			videoCommentMapper.deleteByParam(videoCommentQuery);

			//删除文件
			for (VideoInfoFile item : videoInfoFileList) {
				try {
					FileUtils.deleteDirectory(new File(appConfig.getProjectFolder() + Constants.FILE_FOLDER +item.getFilePath()));//从本地文件夹删除C:\webser\easylive\file\video ------ 的对应文件
				} catch (IOException e) {
					log.error("删除文件失败，文件路径:{}", item.getFilePath());
				}
			}
		});
	}
}