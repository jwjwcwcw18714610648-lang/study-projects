package com.easylive.service.impl;

import java.util.List;

import javax.annotation.Resource;

import com.easylive.component.EsSearchComponent;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.enums.ResponseCodeEnum;
import com.easylive.entity.enums.SearchOrderTypeEnum;
import com.easylive.entity.enums.UserActionTypeEnum;
import com.easylive.entity.po.VideoInfo;
import com.easylive.entity.query.VideoInfoQuery;
import com.easylive.exception.BusinessException;
import com.easylive.mappers.VideoInfoMapper;
import org.springframework.stereotype.Service;

import com.easylive.entity.enums.PageSize;
import com.easylive.entity.query.VideoDanmuQuery;
import com.easylive.entity.po.VideoDanmu;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.entity.query.SimplePage;
import com.easylive.mappers.VideoDanmuMapper;
import com.easylive.service.VideoDanmuService;
import com.easylive.utils.StringTools;
import org.springframework.transaction.annotation.Transactional;


/**
 * 视频弹幕 业务接口实现
 */
@Service("videoDanmuService")
public class VideoDanmuServiceImpl implements VideoDanmuService {

	@Resource
	private VideoDanmuMapper<VideoDanmu, VideoDanmuQuery> videoDanmuMapper;
	@Resource
	private VideoInfoMapper<VideoInfo, VideoInfoQuery> videoInfoMapper;
	@Resource
	private EsSearchComponent esSearchComponent;
	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<VideoDanmu> findListByParam(VideoDanmuQuery param) {
		return this.videoDanmuMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(VideoDanmuQuery param) {
		return this.videoDanmuMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<VideoDanmu> findListByPage(VideoDanmuQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<VideoDanmu> list = this.findListByParam(param);
		PaginationResultVO<VideoDanmu> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void saveVideoDanmu(VideoDanmu bean) {
		VideoInfo videoInfo = videoInfoMapper.selectByVideoId(bean.getVideoId());//先查询视频是否存在
		if (videoInfo == null) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);//不存在就抛出异常
		}
		//是否关闭弹幕
		if (videoInfo.getInteraction() != null && videoInfo.getInteraction().contains("1")) {//如果这个字段中存在1 则说明用户投稿时已经 抛弃了弹幕功能
			throw new BusinessException("UP主已关闭弹幕");
		}
		this.videoDanmuMapper.insert(bean);//插入到弹幕表即可
		this.videoInfoMapper.updateCountInfo(bean.getVideoId(), UserActionTypeEnum.VIDEO_DANMU.getField(), 1);
		// 更新视频表中的弹幕数量 参数 ： 视频id 更改的字段 danmu_count 更新的数量为1个
		esSearchComponent.updateDocCount(bean.getVideoId(), SearchOrderTypeEnum.VIDEO_DANMU.getField(), 1);

	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(VideoDanmu bean) {
		return this.videoDanmuMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<VideoDanmu> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.videoDanmuMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<VideoDanmu> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.videoDanmuMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(VideoDanmu bean, VideoDanmuQuery param) {
		StringTools.checkParam(param);
		return this.videoDanmuMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(VideoDanmuQuery param) {
		StringTools.checkParam(param);
		return this.videoDanmuMapper.deleteByParam(param);
	}

	/**
	 * 根据DanmuId获取对象
	 */
	@Override
	public VideoDanmu getVideoDanmuByDanmuId(Integer danmuId) {
		return this.videoDanmuMapper.selectByDanmuId(danmuId);
	}

	/**
	 * 根据DanmuId修改
	 */
	@Override
	public Integer updateVideoDanmuByDanmuId(VideoDanmu bean, Integer danmuId) {
		return this.videoDanmuMapper.updateByDanmuId(bean, danmuId);
	}

	/**
	 * 根据DanmuId删除
	 */
	@Override
	public Integer deleteVideoDanmuByDanmuId(Integer danmuId) {
		return this.videoDanmuMapper.deleteByDanmuId(danmuId);
	}
	@Override
	public void deleteDanmu(String userId, Integer danmuId) {
		VideoDanmu danmu = videoDanmuMapper.selectByDanmuId(danmuId);
		if (null == danmu) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		VideoInfo videoInfo = videoInfoMapper.selectByVideoId(danmu.getVideoId());
		if (null == videoInfo) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}

		if (userId != null && !videoInfo.getUserId().equals(userId)) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		videoDanmuMapper.deleteByDanmuId(danmuId);
	}
}