package com.easylive.service.impl;

import com.easylive.component.RedisComponent;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.enums.PageSize;
import com.easylive.entity.enums.StatisticsTypeEnum;
import com.easylive.entity.enums.UserActionTypeEnum;
import com.easylive.entity.po.StatisticsInfo;
import com.easylive.entity.po.UserFocus;
import com.easylive.entity.po.VideoInfo;
import com.easylive.entity.query.SimplePage;
import com.easylive.entity.query.StatisticsInfoQuery;
import com.easylive.entity.query.UserInfoQuery;
import com.easylive.entity.query.VideoInfoQuery;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.mappers.StatisticsInfoMapper;
import com.easylive.mappers.UserFocusMapper;
import com.easylive.mappers.UserInfoMapper;
import com.easylive.mappers.VideoInfoMapper;
import com.easylive.service.StatisticsInfoService;
import com.easylive.service.UserInfoService;
import com.easylive.utils.DateUtil;
import com.easylive.utils.StringTools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * 数据统计 业务接口实现
 */
@Service("statisticsInfoService")
public class StatisticsInfoServiceImpl implements StatisticsInfoService {

	@Resource
	private StatisticsInfoMapper<StatisticsInfo, StatisticsInfoQuery> statisticsInfoMapper;
    @Autowired
    private RedisComponent redisComponent;
    @Autowired
    private VideoInfoMapper videoInfoMapper;
	@Resource
	private UserInfoMapper userInfoMapper;
	@Resource
	private UserFocusMapper userFocusMapper;
	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<StatisticsInfo> findListByParam(StatisticsInfoQuery param) {
		return this.statisticsInfoMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(StatisticsInfoQuery param) {
		return this.statisticsInfoMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<StatisticsInfo> findListByPage(StatisticsInfoQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<StatisticsInfo> list = this.findListByParam(param);
		PaginationResultVO<StatisticsInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(StatisticsInfo bean) {
		return this.statisticsInfoMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<StatisticsInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.statisticsInfoMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<StatisticsInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.statisticsInfoMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(StatisticsInfo bean, StatisticsInfoQuery param) {
		StringTools.checkParam(param);
		return this.statisticsInfoMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(StatisticsInfoQuery param) {
		StringTools.checkParam(param);
		return this.statisticsInfoMapper.deleteByParam(param);
	}

	/**
	 * 根据StatisticsDateAndUserIdAndDataType获取对象
	 */
	@Override
	public StatisticsInfo getStatisticsInfoByStatisticsDateAndUserIdAndDataType(String statisticsDate, String userId, Integer dataType) {
		return this.statisticsInfoMapper.selectByStatisticsDateAndUserIdAndDataType(statisticsDate, userId, dataType);
	}

	/**
	 * 根据StatisticsDateAndUserIdAndDataType修改
	 */
	@Override
	public Integer updateStatisticsInfoByStatisticsDateAndUserIdAndDataType(StatisticsInfo bean, String statisticsDate, String userId, Integer dataType) {
		return this.statisticsInfoMapper.updateByStatisticsDateAndUserIdAndDataType(bean, statisticsDate, userId, dataType);
	}

	@Override
	public void statisticsData() {
		List<StatisticsInfo> statisticsInfoList =new ArrayList<>();
		final String statisticsDate= DateUtil.getBeforeDayDate(1);
		//统计播放量
		// 从 Redis 中获取指定日期（statisticsDate）的视频播放量统计，结果以 Map 形式存储，key 为视频ID，value 为播放次数
		Map<String,Integer> videoPlayCountMap = redisComponent.getVideoPlayCount(statisticsDate);
		// 获取所有有播放数据的视频ID列表，用于后续遍历处理
		// keySet()方法会返回map中所有key的集合，这里就是所有有播放量记录的视频ID列表
		List<String> playVideoKeys = new ArrayList<>(videoPlayCountMap.keySet());
		// 遍历所有视频播放量的key（例如格式可能为 "video:play:20240601:12345"），
		// 通过substring截取每个key最后一个冒号后的内容（即视频ID）,
		// 最终获得一个纯粹的视频ID列表
		playVideoKeys = playVideoKeys.stream()
				.map(item -> item.substring(item.lastIndexOf(":") + 1))
				.collect(Collectors.toList());
		// 构建视频查询对象，将所有有播放次数记录的视频ID数组放进去，以便批量查询视频详细信息
		VideoInfoQuery videoInfoQuery = new VideoInfoQuery();
		// playVideoKeys是包含所有有播放数据的视频ID的集合，转array赋值进查询条件
		videoInfoQuery.setVideoIdArray(playVideoKeys.toArray(new String[0]));
		// 根据组装好的查询对象批量查出视频详情列表，后续用于统计播放量等明细
		List<VideoInfo> videoInfoList = videoInfoMapper.selectList(videoInfoQuery);
		// 统计每个用户（userId）对应的总播放量
		// 1. 遍历 videoInfoList（视频详情列表），以视频的 userId 作为分组依据
		// 2. 对同一用户（userId）下的所有视频，求他们的播放量总和
		// 3. 播放量通过 videoPlayCountMap 获取，key 需要拼接 REDIS 前缀 + 日期 + 视频Id，若没有对应记录，播放量按0处理
		Map<String, Integer> videoCountMap = videoInfoList.stream()
			.collect(Collectors.groupingBy(
				VideoInfo::getUserId, // 按用户userId分组
				Collectors.summingInt(item -> { // 统计该用户下所有视频的总播放量
					// 拼接实际的Redis key，获取该视频的播放量
					Integer count = videoPlayCountMap.get(Constants.REDIS_KEY_VIDEO_PLAY_COUNT + statisticsDate + ":" + item.getVideoId());
					return count == null ? 0 : count; // 没有统计则视为0
				})
			));
		/*
		 * 遍历视频播放量统计Map，为每位用户生成统计信息对象，并加入结果列表
		 * 
		 * API:
		 * Map#forEach(BiConsumer<? super K,? super V> action)
		 *  - 这里对每一组<用户userId, 播放量>遍历处理
		 * 
		 * 处理逻辑：
		 *  1. k 代表用户userId，v 代表该用户所有视频的总播放量
		 *  2. 创建一个新的 StatisticsInfo 统计信息对象
		 *     - setStatisticsDate: 统计日期（如"20240601"），用于标记日报归属
		 *     - setUserId: 用户ID，对应某一位主播或内容作者
		 *     - setDataType: 数据类型，这里固定为“播放量”，调用枚举类型StatisticsTypeEnum.PLAY.getType()
		 *     - setStatisticsCount: 统计值，即该用户所有视频的总播放量
		 *  3. 组装好的统计对象放入结果列表statisticsInfoList中，后续可以批量入库或做进一步处理
		 */
		videoCountMap.forEach((userId, playCount) -> {
			StatisticsInfo statisticsInfo = new StatisticsInfo(); // 创建统计实体
			statisticsInfo.setStatisticsDate(statisticsDate);    // 赋值统计日期
			statisticsInfo.setUserId(userId);                    // 赋值用户Id
			statisticsInfo.setDataType(StatisticsTypeEnum.PLAY.getType()); // 设置数据类型为“播放量”
			statisticsInfo.setStatisticsCount(playCount);        // 赋值用户的视频总播放量
			statisticsInfoList.add(statisticsInfo);              // 加入统计列表
		});
		//统计粉丝量
		/*
		 * 下面这段代码用于统计每个用户的“粉丝量”。
		 * 
		 * 1. 先通过 statisticsInfoMapper 的 selectStatisticsFans 方法，将指定统计日期的所有用户新增粉丝量查询出来。
		 *    - 参数 statisticsDate 是当日的日期标识（比如 "20240601"）。
		 *    - selectStatisticsFans 查询返回每一行都是某个用户当天新增的粉丝数量，返回结果是一个 StatisticsInfo 对象列表，每对象对应一个用户。
		 *      其中 user_id 字段映射为 StatisticsInfo.userId，count(1) 统计值映射为 StatisticsInfo.statisticsCount。
		 *
		 * 2. 遍历 fansDataList（即每位用户的粉丝统计对象），给每个 StatisticsInfo 对象补充统计日期和数据类型。
		 *    - 统计日期：通过 statisticsInfo.setStatisticsDate(statisticsDate) 设为当前统计日，便于区分是哪个日期的统计。
		 *    - 数据类型：通过 statisticsInfo.setDataType(StatisticsTypeEnum.FANS.getType())，把数据类型设定为“粉丝量”。
		 *      FANS.getType() 通常返回一个和粉丝统计相关的类型标识（如整数2等）。
		 *
		 * 3. 把组装好的本日所有粉丝量统计对象，批量加入到汇总的统计列表 statisticsInfoList 里面，一起作为后续的统计入库或处理数据。
		 */
		List<StatisticsInfo> fansDataList = this.statisticsInfoMapper.selectStatisticsFans(statisticsDate); // 1. 查询各用户粉丝数据
		for (StatisticsInfo statisticsInfo : fansDataList) {
			statisticsInfo.setStatisticsDate(statisticsDate); // 2a. 设置统计日期
			statisticsInfo.setDataType(StatisticsTypeEnum.FANS.getType()); // 2b. 设置数据类型为“粉丝量”
		}
		statisticsInfoList.addAll(fansDataList); // 3. 批量合并到统计结果集

		//统计评论
		List<StatisticsInfo> commentDataList = this.statisticsInfoMapper.selectStatisticsComment(statisticsDate);
		for (StatisticsInfo statisticsInfo : commentDataList) {
			statisticsInfo.setStatisticsDate(statisticsDate);
			statisticsInfo.setDataType(StatisticsTypeEnum.COMMENT.getType());
		}

		/*
		 * 下面这段代码用于统计“弹幕、点赞、收藏、投币”等用户行为的数据，并进行类型转换和合并到总统计列表中。
		 * 
		 * 1. 首先调用 statisticsInfoMapper.selectStatisticsInfo 方法，查询统计日期对应的各项用户行为的统计数据。
		 *    - 第一个参数 statisticsDate, 表示统计的日期（如 "20240601"），用于筛选当日的数据。
		 *    - 第二个参数为一个 Integer 数组，指定要统计的行为类型，包括：点赞（VIDEO_LIKE）、投币（VIDEO_COIN）、收藏（VIDEO_COLLECT）。
		 *    - 方法返回值为一个 StatisticsInfo 对象列表，每个对象代表一个用户在当日对某类行为的总数。
		 *      例：userId=1001, dataType=VIDEO_LIKE, statisticsCount=8  表示用户1001当天点赞了8次；
		 *          userId=1002, dataType=VIDEO_COIN, statisticsCount=11 表示用户1002当天投币11次。
		 * 
		 * 2. 遍历统计结果 statisticsInfoOthers, 针对每个数据对象做进一步处理：
		 *    - a) 统一设置统计日期 statisticsInfo.setStatisticsDate(statisticsDate), 便于后续数据归档与区分。
		 *    - b) 对原始的数据类型 dataType 做规范化转换：
		 *         · selectStatisticsInfo 查询回来时 dataType 字段值是 USER_ACTION_TYPE 枚举下的类型，比如 VIDEO_LIKE(1)、VIDEO_COLLECT(2)、VIDEO_COIN(3)等。
		 *         · 需要把这些类型映射为统一统计用的类型，即 StatisticsTypeEnum 下的 LIKE、COLLECTION、COIN。
		 *         · 若当前 statisticsInfo.getDataType() 等于 VIDEO_LIKE, 就转换为 LIKE 类型；
		 *           若等于 VIDEO_COLLECT, 就转换为 COLLECTION 类型；
		 *           若等于 VIDEO_COIN, 就转换为 COIN 类型。
		 *       这样做的目的是规范化后续统计和入库的统计类型，便于统一检索分析。
		 * 
		 * 3. 把处理好的 statisticsInfoOthers 列表合并到总统计列表 statisticsInfoList 中，方便最后统一批量写入数据库。
		 */
		List<StatisticsInfo> statisticsInfoOthers = this.statisticsInfoMapper.selectStatisticsInfo(
				statisticsDate,
				new Integer[]{
						UserActionTypeEnum.VIDEO_LIKE.getType(),     // 点赞
						UserActionTypeEnum.VIDEO_COIN.getType(),     // 投币
						UserActionTypeEnum.VIDEO_COLLECT.getType()   // 收藏
				}
		);

		for (StatisticsInfo statisticsInfo : statisticsInfoOthers) {
			// 设置统计日期，确保每一条统计数据归属到正确的日期
			statisticsInfo.setStatisticsDate(statisticsDate);

			// 对原始的用户行为类型进行规范化处理，转换为统一的统计数据类型
			if (UserActionTypeEnum.VIDEO_LIKE.getType().equals(statisticsInfo.getDataType())) {
				// 如果是点赞行为，设置为“LIKE”统计类型
				statisticsInfo.setDataType(StatisticsTypeEnum.LIKE.getType());
			} else if (UserActionTypeEnum.VIDEO_COLLECT.getType().equals(statisticsInfo.getDataType())) {
				// 如果是收藏行为，设置为“COLLECTION”统计类型
				statisticsInfo.setDataType(StatisticsTypeEnum.COLLECTION.getType());
			} else if (UserActionTypeEnum.VIDEO_COIN.getType().equals(statisticsInfo.getDataType())) {
				// 如果是投币行为，设置为“COIN”统计类型
				statisticsInfo.setDataType(StatisticsTypeEnum.COIN.getType());
			}
		}

		// 最后，把所有这些数据批量加到统计结果汇总列表，方便一起批量入库
		statisticsInfoList.addAll(statisticsInfoOthers);
		this.statisticsInfoMapper.insertOrUpdateBatch(statisticsInfoList);
	}
	/**
	 * 根据StatisticsDateAndUserIdAndDataType删除
	 */
	@Override
	public Integer deleteStatisticsInfoByStatisticsDateAndUserIdAndDataType(String statisticsDate, String userId, Integer dataType) {
		return this.statisticsInfoMapper.deleteByStatisticsDateAndUserIdAndDataType(statisticsDate, userId, dataType);
	}
	@Override
	public Map<String, Integer> getStatisticsInfoActualTime(String userId) {
		Map<String, Integer> result = statisticsInfoMapper.selectTotalCountInfo(userId);
		if (!StringTools.isEmpty(userId)) {
			//查询粉丝数
			result.put("userCount", userFocusMapper.selectFansCount(userId));
		} else {
			result.put("userCount", userInfoMapper.selectCount(new UserInfoQuery()));
		}
		return result;
	}
	@Override
	public List<StatisticsInfo> findListTotalInfoByParam(StatisticsInfoQuery param) {
		return statisticsInfoMapper.selectListTotalInfoByParam(param);
	}

	@Override
	public List<StatisticsInfo> findUserCountTotalInfoByParam(StatisticsInfoQuery param) {
		return statisticsInfoMapper.selectUserCountTotalInfoByParam(param);
	}
}