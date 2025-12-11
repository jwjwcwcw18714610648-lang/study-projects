package com.easylive.service.impl;

import com.easylive.api.consumer.VideoClient;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.enums.CommentTopTypeEnum;
import com.easylive.entity.enums.PageSize;
import com.easylive.entity.enums.ResponseCodeEnum;
import com.easylive.entity.enums.UserActionTypeEnum;
import com.easylive.entity.po.UserInfo;
import com.easylive.entity.po.VideoComment;
import com.easylive.entity.po.VideoInfo;
import com.easylive.entity.query.SimplePage;
import com.easylive.entity.query.VideoCommentQuery;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.exception.BusinessException;

import com.easylive.mappers.VideoCommentMapper;

import com.easylive.service.VideoCommentService;
import com.easylive.utils.StringTools;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;


/**
 * 评论 业务接口实现
 */
@Service("videoCommentService")
public class VideoCommentServiceImpl implements VideoCommentService {

	@Resource
	private VideoClient videoClient;
	@Resource
	private VideoCommentMapper<VideoComment, VideoCommentQuery> videoCommentMapper;


	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<VideoComment> findListByParam(VideoCommentQuery param) {
		if(param.getLoadChildren()!=null&& param.getLoadChildren()){
			return this.videoCommentMapper.selectListWithChildren(param);
		}
		return this.videoCommentMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(VideoCommentQuery param) {
		return this.videoCommentMapper.selectCount(param);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void topComment(Integer commentId, String userId) {
		this.cancelTopComment(commentId,userId);//删除当前所有置顶的评论
		VideoComment videoComment=new VideoComment();
		videoComment.setTopType(CommentTopTypeEnum.TOP.getType());
		videoCommentMapper.updateByCommentId(videoComment,commentId);

	}

	@Override
	public void cancelTopComment(Integer commentId, String userId) {
	VideoComment videoComment=videoCommentMapper.selectByCommentId(commentId);//查对应id的评论
		if(videoComment==null){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		VideoInfo videoInfo= (VideoInfo) videoClient.getVideoInfoByVideoId(videoComment.getVideoId());//查到对应评论的所处在的视频信息
		if(videoInfo==null)throw new BusinessException(ResponseCodeEnum.CODE_600);
		if(!videoInfo.getUserId().equals(userId))throw new BusinessException(ResponseCodeEnum.CODE_600);//视频的作者和操作的作者并非一个人
		VideoComment videoComment1 =new VideoComment();
		videoComment1.setTopType(CommentTopTypeEnum.NO_TOP.getType());//初始为未置顶
		VideoCommentQuery videoCommentQuery=new VideoCommentQuery();
		videoCommentQuery.setTopType(CommentTopTypeEnum.TOP.getType());
		videoCommentQuery.setVideoId(videoInfo.getVideoId());
		videoCommentMapper.updateByParam(videoComment1,videoCommentQuery);

	}

	@Override
	public void deleteComment(Integer commentId, String userId) {
		VideoComment videoComment = videoCommentMapper.selectByCommentId(commentId);
		if(videoComment==null)throw new BusinessException(ResponseCodeEnum.CODE_600);//评论不存在
		VideoInfo videoInfo= (VideoInfo) videoClient.getVideoInfoByVideoId(videoComment.getVideoId());//查询视频信息
		if(videoInfo==null)throw new BusinessException(ResponseCodeEnum.CODE_600);//视频不存在
	if(!userId.equals(videoInfo.getUserId())&&!userId.equals(videoComment.getUserId()))throw new BusinessException(ResponseCodeEnum.CODE_600);
		videoCommentMapper.deleteByCommentId(commentId);//删除该文件
		//删除二级评论
		if(videoComment.getpCommentId()=='0'){//说明其为父评论
			videoClient.updateCountInfo(videoInfo.getVideoId(),UserActionTypeEnum.VIDEO_COMMENT.getField(),-1);//视频评论数据减去1
			VideoCommentQuery videoCommentQuery=new VideoCommentQuery();
			videoCommentQuery.setpCommentId(videoComment.getpCommentId());
			videoCommentMapper.deleteByParam(videoCommentQuery);

		}
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<VideoComment> findListByPage(VideoCommentQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<VideoComment> list = this.findListByParam(param);
		PaginationResultVO<VideoComment> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(VideoComment bean) {
		return this.videoCommentMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<VideoComment> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.videoCommentMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<VideoComment> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.videoCommentMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(VideoComment bean, VideoCommentQuery param) {
		StringTools.checkParam(param);
		return this.videoCommentMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(VideoCommentQuery param) {
		StringTools.checkParam(param);
		return this.videoCommentMapper.deleteByParam(param);
	}

	/**
	 * 根据CommentId获取对象
	 */
	@Override
	public VideoComment getVideoCommentByCommentId(Integer commentId) {
		return this.videoCommentMapper.selectByCommentId(commentId);
	}

	/**
	 * 根据CommentId修改
	 */
	@Override
	public Integer updateVideoCommentByCommentId(VideoComment bean, Integer commentId) {
		return this.videoCommentMapper.updateByCommentId(bean, commentId);
	}

	/**
	 * 根据CommentId删除
	 */
	@Override
	public Integer deleteVideoCommentByCommentId(Integer commentId) {
		return this.videoCommentMapper.deleteByCommentId(commentId);
	}
	@Override
	@GlobalTransactional(rollbackFor = Exception.class)
	public void postComment(VideoComment comment, Integer replyCommentId) {

		VideoInfo videoInfo = (VideoInfo) videoClient.getVideoInfoByVideoId(comment.getVideoId());
		if (videoInfo == null) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		//是否关闭评论
		if (videoInfo.getInteraction() != null && videoInfo.getInteraction().contains(Constants.ZERO.toString())) {
			throw new BusinessException("UP主已关闭评论区");
		}
		if (replyCommentId != null) {
			VideoComment replyComment = getVideoCommentByCommentId(replyCommentId);//查出回复的评论 （即上级评论
			if (replyComment == null || !replyComment.getVideoId().equals(comment.getVideoId())) {//不能跨视频；【评论
				throw new BusinessException(ResponseCodeEnum.CODE_600);
			}
			if (replyComment.getpCommentId() == 0) {//说明回复的是一个评论
				comment.setpCommentId(replyComment.getCommentId());//那么父id即为上一级的id
			} else {
				comment.setpCommentId(replyComment.getpCommentId());//此else 说明是回复 回复别的评论的 评论 直接挂在最高一级评论上 （因为做的是二级评论 多级也要变成二级）
				comment.setReplyUserId(replyComment.getUserId());
			}
			UserInfo userInfo = (UserInfo) videoClient.getUserInfoByUserId(replyComment.getUserId());//查上级评论的 信息 昵称与头像
			comment.setReplyNickName(userInfo.getNickName());
			comment.setReplyAvatar(userInfo.getAvatar());
		} else {
			comment.setpCommentId(0);//回复id如果为空的话 显然父id为0
		}
		comment.setPostTime(new Date());
		comment.setVideoUserId(videoInfo.getUserId());
		this.videoCommentMapper.insert(comment);
		//增加评论数
		if (comment.getpCommentId() == 0) {
			this.videoClient.updateCountInfo(comment.getVideoId(), UserActionTypeEnum.VIDEO_COMMENT.getField(), 1);//只增加父id为0的评论数

		}

	}
}