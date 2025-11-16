package com.easylive.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import com.easylive.entity.enums.ResponseCodeEnum;
import com.easylive.entity.po.UserVideoSeriesVideo;
import com.easylive.entity.query.UserVideoSeriesVideoQuery;
import com.easylive.entity.query.VideoInfoQuery;
import com.easylive.exception.BusinessException;
import com.easylive.mappers.UserVideoSeriesVideoMapper;
import com.easylive.mappers.VideoInfoMapper;
import org.springframework.stereotype.Service;

import com.easylive.entity.enums.PageSize;
import com.easylive.entity.query.UserVideoSeriesQuery;
import com.easylive.entity.po.UserVideoSeries;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.entity.query.SimplePage;
import com.easylive.mappers.UserVideoSeriesMapper;
import com.easylive.service.UserVideoSeriesService;
import com.easylive.utils.StringTools;
import org.springframework.transaction.annotation.Transactional;


/**
 * 用户视频序列归档 业务接口实现
 */
@Service("userVideoSeriesService")
public class UserVideoSeriesServiceImpl implements UserVideoSeriesService {

	@Resource
	private UserVideoSeriesMapper<UserVideoSeries, UserVideoSeriesQuery> userVideoSeriesMapper;
	@Resource
	private VideoInfoMapper videoInfoMapper;
	@Resource
	private UserVideoSeriesVideoMapper userVideoSeriesVideoMapper;
	@Override
	public void changeVideoSeriesSort(String userId, String seriesIds) {
		String[] seriesIdArray = seriesIds.split(",");
		List<UserVideoSeries> videoSeriesList = new ArrayList<>();
		Integer sort = 0;
		for (String seriesId : seriesIdArray) {
			UserVideoSeries videoSeries = new UserVideoSeries();
			videoSeries.setUserId(userId);
			videoSeries.setSeriesId(Integer.parseInt(seriesId));
			videoSeries.setSort(++sort);
			videoSeriesList.add(videoSeries);
		}//填入将排列好的id 以及信息排序放进list表中
		userVideoSeriesMapper.changeSort(videoSeriesList);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<UserVideoSeries> findListByParam(UserVideoSeriesQuery param) {
		return this.userVideoSeriesMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(UserVideoSeriesQuery param) {
		return this.userVideoSeriesMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<UserVideoSeries> findListByPage(UserVideoSeriesQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<UserVideoSeries> list = this.findListByParam(param);
		PaginationResultVO<UserVideoSeries> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(UserVideoSeries bean) {
		return this.userVideoSeriesMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<UserVideoSeries> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userVideoSeriesMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<UserVideoSeries> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userVideoSeriesMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(UserVideoSeries bean, UserVideoSeriesQuery param) {
		StringTools.checkParam(param);
		return this.userVideoSeriesMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(UserVideoSeriesQuery param) {
		StringTools.checkParam(param);
		return this.userVideoSeriesMapper.deleteByParam(param);
	}

	/**
	 * 根据SeriesId获取对象
	 */
	@Override
	public UserVideoSeries getUserVideoSeriesBySeriesId(Integer seriesId) {
		return this.userVideoSeriesMapper.selectBySeriesId(seriesId);
	}

	/**
	 * 根据SeriesId修改
	 */
	@Override
	public Integer updateUserVideoSeriesBySeriesId(UserVideoSeries bean, Integer seriesId) {
		return this.userVideoSeriesMapper.updateBySeriesId(bean, seriesId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void saveUserVideoSeries(UserVideoSeries bean, String videoIds) {
		if (bean.getSeriesId() == null && StringTools.isEmpty(videoIds)) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		if (bean.getSeriesId() == null) {//说明是新增
			checkVideoIds(bean.getUserId(), videoIds);
			bean.setUpdateTime(new Date());
			bean.setSort(this.userVideoSeriesMapper.selectMaxSort(bean.getUserId()) + 1);
			this.userVideoSeriesMapper.insert(bean);
			this.saveSeriesVideo(bean.getUserId(), bean.getSeriesId(), videoIds);
		} else {
			UserVideoSeriesQuery seriesQuery = new UserVideoSeriesQuery();
			seriesQuery.setUserId(bean.getUserId());
			seriesQuery.setSeriesId(bean.getSeriesId());
			this.userVideoSeriesMapper.updateByParam(bean, seriesQuery);
		}
	}
	//校验视频id
	private void checkVideoIds(String userId, String videoIds) {
		String[] videoIdArray = videoIds.split(",");//分割前端传来的id集合 装入array
		VideoInfoQuery videoInfoQuery = new VideoInfoQuery();
		videoInfoQuery.setVideoIdArray(videoIdArray);
		videoInfoQuery.setUserId(userId);
		Integer count = videoInfoMapper.selectCount(videoInfoQuery);//count应该返回的是videoids 中的id 数
		if (videoIdArray.length != count) {//如果根事实不符 说明发生了异常
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void delVideoSeries(String userId, Integer seriesId) {//依旧鉴权 随后传入待删除的分类id
		UserVideoSeriesQuery seriesQuery = new UserVideoSeriesQuery();
		seriesQuery.setUserId(userId);
		seriesQuery.setSeriesId(seriesId);
		Integer count = userVideoSeriesMapper.deleteByParam(seriesQuery);//先删除分类表
		if (count == 0) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);//未删除成功 或者根本不存在传输过来的表的数据 直接抛异常
		}
		UserVideoSeriesVideoQuery videoSeriesVideoQuery = new UserVideoSeriesVideoQuery();//再删除分类表对应的视频表
		videoSeriesVideoQuery.setSeriesId(seriesId);
		videoSeriesVideoQuery.setUserId(userId);
		userVideoSeriesVideoMapper.deleteByParam(videoSeriesVideoQuery);
	}

	@Override
	public List<UserVideoSeries> findListWithVideoList(UserVideoSeriesQuery query) {//根据传入的series表数据关联查
		return userVideoSeriesMapper.selectListWithVideoList(query);
	}

	@Override
	public void delSeriesVideo(String userId, Integer seriesId, String videoId) {//鉴权自己是作者本人 传入分类id 视频id
		UserVideoSeriesVideoQuery videoSeriesVideoQuery = new UserVideoSeriesVideoQuery();
		videoSeriesVideoQuery.setUserId(userId);
		videoSeriesVideoQuery.setSeriesId(seriesId);
		videoSeriesVideoQuery.setVideoId(videoId);
		this.userVideoSeriesVideoMapper.deleteByParam(videoSeriesVideoQuery);//直接删除视频 并不需要删除分类的表 哪怕使其为空
	}

	@Override
	public void saveSeriesVideo(String userId, Integer seriesId, String videoIds) {
		UserVideoSeries userVideoSeries = getUserVideoSeriesBySeriesId(seriesId);
		if (!userVideoSeries.getUserId().equals(userId)) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		checkVideoIds(userId, videoIds);
		String[] videoIdArray = videoIds.split(",");
		Integer sort = this.userVideoSeriesVideoMapper.selectMaxSort(seriesId);
		List<UserVideoSeriesVideo> seriesVideoList = new ArrayList<>();
		for (String videoId : videoIdArray) {
			UserVideoSeriesVideo videoSeriesVideo = new UserVideoSeriesVideo();
			videoSeriesVideo.setVideoId(videoId);
			videoSeriesVideo.setSort(++sort);
			videoSeriesVideo.setSeriesId(seriesId);
			videoSeriesVideo.setUserId(userId);
			seriesVideoList.add(videoSeriesVideo);
		}
		this.userVideoSeriesVideoMapper.insertOrUpdateBatch(seriesVideoList);//会根据唯一索引判断记录是否存在，存在则更新，不存在则插入 主键有二：seriseid 视频id ids序列不同 保存的视频顺序也就不同
	}
	@Override
	public List<UserVideoSeries> getUserAllSeries(String userId) {
		return userVideoSeriesMapper.selectUserAllSeries(userId);
	}

	/**
	 * 根据SeriesId删除
	 */
	@Override
	public Integer deleteUserVideoSeriesBySeriesId(Integer seriesId) {
		return this.userVideoSeriesMapper.deleteBySeriesId(seriesId);
	}
}