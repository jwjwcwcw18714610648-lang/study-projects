package com.easylive.service.impl;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.LinkedTransferQueue;

import javax.annotation.Resource;

import com.easylive.entity.dto.UserMessageCountDto;
import com.easylive.entity.dto.UserMessageExtendDto;
import com.easylive.entity.enums.MessageReadTypeEnum;
import com.easylive.entity.enums.MessageTypeEnum;
import com.easylive.entity.po.VideoComment;
import com.easylive.entity.po.VideoInfo;
import com.easylive.entity.po.VideoInfoPost;
import com.easylive.entity.query.VideoCommentQuery;
import com.easylive.entity.query.VideoInfoPostQuery;
import com.easylive.mappers.VideoCommentMapper;
import com.easylive.mappers.VideoInfoMapper;
import com.easylive.mappers.VideoInfoPostMapper;
import com.easylive.utils.JsonUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.lucene.util.ArrayUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.easylive.entity.enums.PageSize;
import com.easylive.entity.query.UserMessageQuery;
import com.easylive.entity.po.UserMessage;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.entity.query.SimplePage;
import com.easylive.mappers.UserMessageMapper;
import com.easylive.service.UserMessageService;
import com.easylive.utils.StringTools;


/**
 * 用户消息表 业务接口实现
 */
@Service("userMessageService")
public class UserMessageServiceImpl implements UserMessageService {

	@Resource
	private UserMessageMapper<UserMessage, UserMessageQuery> userMessageMapper;


	@Resource
	private VideoInfoPostMapper<VideoInfoPost, VideoInfoPostQuery> videoInfoPostMapper;

	@Resource
	private VideoCommentMapper<VideoComment, VideoCommentQuery> videoCommentMapper;
    @Autowired
    private VideoInfoMapper videoInfoMapper;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<UserMessage> findListByParam(UserMessageQuery param) {
		return this.userMessageMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(UserMessageQuery param) {
		return this.userMessageMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<UserMessage> findListByPage(UserMessageQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<UserMessage> list = this.findListByParam(param);
		PaginationResultVO<UserMessage> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(UserMessage bean) {
		return this.userMessageMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<UserMessage> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userMessageMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<UserMessage> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userMessageMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(UserMessage bean, UserMessageQuery param) {
		StringTools.checkParam(param);
		return this.userMessageMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(UserMessageQuery param) {
		StringTools.checkParam(param);
		return this.userMessageMapper.deleteByParam(param);
	}

	/**
	 * 根据MessageId获取对象
	 */
	@Override
	public UserMessage getUserMessageByMessageId(Integer messageId) {
		return this.userMessageMapper.selectByMessageId(messageId);
	}

	/**
	 * 根据MessageId修改
	 */
	@Override
	public Integer updateUserMessageByMessageId(UserMessage bean, Integer messageId) {
		return this.userMessageMapper.updateByMessageId(bean, messageId);
	}

	@Override
	public List<UserMessageCountDto> getMessageTypeNoReadCount(String userId) {
		return userMessageMapper.getMessageTypeNoReadCount(userId);
	}

	@Override
	public void saveUserMessage(String videoId, String sendUserId, MessageTypeEnum messageTypeEnum, String content, Integer replyCommentId) {
		VideoInfo videoInfo= (VideoInfo) videoInfoMapper.selectByVideoId(videoId);
		if(null==videoInfo)
		{
			return;
		}
		UserMessageExtendDto extendDto=new UserMessageExtendDto();
		extendDto.setMessageContent(content);
		String userId=videoInfo.getUserId();//获取收到信息人的id
		//对于重复点赞 收藏 不做通知
		if(ArrayUtils.contains(new int[]{MessageTypeEnum.COLLECTION.getType(),MessageTypeEnum.LIKE.getType()},messageTypeEnum.getType()))//查操作的类型
		{
			UserMessageQuery userMessageQuery=new UserMessageQuery();
			userMessageQuery.setMessageType(messageTypeEnum.getType());
			userMessageQuery.setSendUserId(sendUserId);
			userMessageQuery.setVideoId(videoId);
			Integer count = userMessageMapper.selectCount(userMessageQuery);
			if(count>0){
				return;// 说明多次操作
			}
		}
		UserMessage userMessage = new UserMessage();
		userMessage.setUserId(userId);
		userMessage.setVideoId(videoId);
		userMessage.setReadType(MessageReadTypeEnum.NO_READ.getType());
		userMessage.setCreateTime(new Date());
		userMessage.setMessageType(messageTypeEnum.getType());
		userMessage.setSendUserId(sendUserId);
		//评论特殊处理
		if(replyCommentId!=null){
			VideoComment commentInfo=videoCommentMapper.selectByCommentId(replyCommentId);
			if(null!=commentInfo){
				userId=commentInfo.getUserId();//父评论接受信息
				extendDto.setMessageContent(content);
			}
		}
		if(userId.equals(sendUserId)){
			return;//自己给自己发评论 不提醒
		}
		//处理系统消息
		if (MessageTypeEnum.SYS == messageTypeEnum) {
			VideoInfoPost videoInfoPost = videoInfoPostMapper.selectByVideoId(videoId);
			extendDto.setAuditStatus(videoInfoPost.getStatus());
		}
		userMessage.setUserId(userId);
		userMessage.setExtendJson(JsonUtils.convertObj2Json(extendDto));
		this.userMessageMapper.insert(userMessage);
	}

	/**
	 * 根据MessageId删除
	 */
	@Override
	public Integer deleteUserMessageByMessageId(Integer messageId) {
		return this.userMessageMapper.deleteByMessageId(messageId);
	}
}