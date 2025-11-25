package com.easylive.service.impl;

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import com.easylive.component.EsSearchComponent;
import com.easylive.entity.enums.ResponseCodeEnum;
import com.easylive.entity.enums.SearchOrderTypeEnum;
import com.easylive.entity.enums.UserActionTypeEnum;
import com.easylive.entity.po.UserInfo;
import com.easylive.entity.po.VideoComment;
import com.easylive.entity.po.VideoInfo;
import com.easylive.entity.query.UserInfoQuery;
import com.easylive.exception.BusinessException;
import com.easylive.mappers.UserInfoMapper;
import com.easylive.mappers.VideoCommentMapper;
import com.easylive.mappers.VideoInfoMapper;
import org.springframework.stereotype.Service;

import com.easylive.entity.enums.PageSize;
import com.easylive.entity.query.UserActionQuery;
import com.easylive.entity.po.UserAction;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.entity.query.SimplePage;
import com.easylive.mappers.UserActionMapper;
import com.easylive.service.UserActionService;
import com.easylive.utils.StringTools;
import org.springframework.transaction.annotation.Transactional;


/**
 * 用户行为 点赞、评论 业务接口实现
 */
@Service("userActionService")
public class UserActionServiceImpl implements UserActionService {
	@Resource
	private UserInfoMapper userInfoMapper;
	@Resource
	private UserActionMapper<UserAction, UserActionQuery> userActionMapper;
	@Resource
	private VideoInfoMapper videoInfoMapper;
	@Resource
	private VideoCommentMapper videoCommentMapper;
	@Resource
	private EsSearchComponent esSearchComponent;
	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<UserAction> findListByParam(UserActionQuery param) {
		return this.userActionMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(UserActionQuery param) {
		return this.userActionMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<UserAction> findListByPage(UserActionQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<UserAction> list = this.findListByParam(param);
		PaginationResultVO<UserAction> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(UserAction bean) {
		return this.userActionMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<UserAction> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userActionMapper.insertBatch(listBean);
	}
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void saveAction(UserAction bean) {
		VideoInfo videoInfo = (VideoInfo) videoInfoMapper.selectByVideoId(bean.getVideoId());
		if (videoInfo == null) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		bean.setVideoUserId(videoInfo.getUserId());

		UserActionTypeEnum actionTypeEnum = UserActionTypeEnum.getByType(bean.getActionType());
		if (actionTypeEnum == null) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}

		UserAction dbAction = userActionMapper.selectByVideoIdAndCommentIdAndActionTypeAndUserId(bean.getVideoId(), bean.getCommentId(), bean.getActionType(),
				bean.getUserId());


		bean.setActionTime(new Date());
		switch (actionTypeEnum) {
			//点赞,收藏
			case VIDEO_LIKE:
			case VIDEO_COLLECT://两个case合并处理 因为逻辑相似
				if (dbAction != null) {//如果查到的已有行为记录存在 则说明是重复点击 则删除原纪录
					userActionMapper.deleteByActionId(dbAction.getActionId());
				} else {
					userActionMapper.insert(bean);
				}
				Integer changeCount = dbAction == null ? 1 : -1;
				videoInfoMapper.updateCountInfo(bean.getVideoId(), actionTypeEnum.getField(), changeCount);//借用之前在xml实现的弹幕加一功能实现点赞的增加

				if (actionTypeEnum == UserActionTypeEnum.VIDEO_COLLECT) {
					esSearchComponent.updateDocCount(videoInfo.getVideoId(),SearchOrderTypeEnum.VIDEO_COLLECT.getField(), changeCount);
				}
				break;
			case VIDEO_COIN:
				if (videoInfo.getUserId().equals(bean.getUserId())) {
					throw new BusinessException("UP主不能给自己投币");
				}
				if (dbAction != null) {
					throw new BusinessException("对本稿件的投币枚数已用完");
				}
				//减少自己的硬币
				Integer updateCount = userInfoMapper.updateCoinCountInfo(bean.getUserId(), -bean.getActionCount());
				if (updateCount == 0) {
					throw new BusinessException("币不够");
				}
				//给up主投币
				updateCount = userInfoMapper.updateCoinCountInfo(videoInfo.getUserId(), bean.getActionCount());
				if (updateCount == 0) {
					throw new BusinessException("投币失败");
				}
				userActionMapper.insert(bean);
				videoInfoMapper.updateCountInfo(bean.getVideoId(), actionTypeEnum.getField(), bean.getActionCount());
				break;
			case COMMENT_LIKE:
			case COMMENT_HATE://如果选择了点赞评论 我们所作的应该将点赞与不喜欢做一个互斥操作
				UserActionTypeEnum opposeTypeEnum = UserActionTypeEnum.COMMENT_LIKE == actionTypeEnum ? UserActionTypeEnum.COMMENT_HATE : UserActionTypeEnum.COMMENT_LIKE;//拿出与当前动作相反的行为 点赞就输出不喜欢 不i喜欢就输出点赞
				UserAction opposeAction = userActionMapper.selectByVideoIdAndCommentIdAndActionTypeAndUserId(bean.getVideoId(), bean.getCommentId(),
						opposeTypeEnum.getType(), bean.getUserId());//查询这个相反的行为是否出现在数据表中
				if (opposeAction != null) {//如果存在直接删除他
					userActionMapper.deleteByActionId(opposeAction.getActionId());
				}

				if (dbAction != null) {
					userActionMapper.deleteByActionId(dbAction.getActionId());//如果当前行为存在 说明是重复点击 则进行删除操作

				} else {
					userActionMapper.insert(bean);//否则插入数据
				}
				changeCount = dbAction == null ? 1 : -1;//如果非重复点击 则应在数据库中加一 否则则应该取消一个1
				Integer opposeChangeCount = changeCount * -1;//其为相反面 则无论如何 相反面都会消失 则始终为负数
				videoCommentMapper.updateCountInfo(bean.getCommentId(),
						actionTypeEnum.getField(),
						changeCount,
						opposeAction == null ? null : opposeTypeEnum.getField(),
						opposeChangeCount);
				break;
		}
	}
	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<UserAction> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userActionMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(UserAction bean, UserActionQuery param) {
		StringTools.checkParam(param);
		return this.userActionMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(UserActionQuery param) {
		StringTools.checkParam(param);
		return this.userActionMapper.deleteByParam(param);
	}

	/**
	 * 根据ActionId获取对象
	 */
	@Override
	public UserAction getUserActionByActionId(Integer actionId) {
		return this.userActionMapper.selectByActionId(actionId);
	}

	/**
	 * 根据ActionId修改
	 */
	@Override
	public Integer updateUserActionByActionId(UserAction bean, Integer actionId) {
		return this.userActionMapper.updateByActionId(bean, actionId);
	}

	/**
	 * 根据ActionId删除
	 */
	@Override
	public Integer deleteUserActionByActionId(Integer actionId) {
		return this.userActionMapper.deleteByActionId(actionId);
	}

	/**
	 * 根据VideoIdAndCommentIdAndActionTypeAndUserId获取对象
	 */
	@Override
	public UserAction getUserActionByVideoIdAndCommentIdAndActionTypeAndUserId(String videoId, Integer commentId, Integer actionType, String userId) {
		return this.userActionMapper.selectByVideoIdAndCommentIdAndActionTypeAndUserId(videoId, commentId, actionType, userId);
	}

	/**
	 * 根据VideoIdAndCommentIdAndActionTypeAndUserId修改
	 */
	@Override
	public Integer updateUserActionByVideoIdAndCommentIdAndActionTypeAndUserId(UserAction bean, String videoId, Integer commentId, Integer actionType, String userId) {
		return this.userActionMapper.updateByVideoIdAndCommentIdAndActionTypeAndUserId(bean, videoId, commentId, actionType, userId);
	}

	/**
	 * 根据VideoIdAndCommentIdAndActionTypeAndUserId删除
	 */
	@Override
	public Integer deleteUserActionByVideoIdAndCommentIdAndActionTypeAndUserId(String videoId, Integer commentId, Integer actionType, String userId) {
		return this.userActionMapper.deleteByVideoIdAndCommentIdAndActionTypeAndUserId(videoId, commentId, actionType, userId);
	}
}