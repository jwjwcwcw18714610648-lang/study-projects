package com.easylive.service.impl;

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import com.easylive.component.RedisComponent;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.dto.CountInfoDto;
import com.easylive.entity.dto.TokenUserInfoDto;
import com.easylive.entity.enums.ResponseCodeEnum;
import com.easylive.entity.enums.UserSexEnum;
import com.easylive.entity.po.UserCountInfoDto;
import com.easylive.entity.po.UserFocus;
import com.easylive.exception.BusinessException;
import com.easylive.mappers.UserFocusMapper;
import com.easylive.mappers.VideoInfoMapper;
import com.easylive.utils.CopyTools;
import org.springframework.stereotype.Service;

import com.easylive.entity.enums.PageSize;
import com.easylive.entity.query.UserInfoQuery;
import com.easylive.entity.po.UserInfo;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.entity.query.SimplePage;
import com.easylive.mappers.UserInfoMapper;
import com.easylive.service.UserInfoService;
import com.easylive.utils.StringTools;
import org.springframework.transaction.annotation.Transactional;


/**
 * 用户信息 业务接口实现
 */
@Service("userInfoService")
public class UserInfoServiceImpl implements UserInfoService {
	@Resource
	private UserFocusMapper userFocusMapper;
	@Resource
	private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;
	@Resource
	private VideoInfoMapper videoInfoMapper;
@Resource
private RedisComponent redisComponent;
	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<UserInfo> findListByParam(UserInfoQuery param) {
		return this.userInfoMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(UserInfoQuery param) {
		return this.userInfoMapper.selectCount(param);
	}

	@Override
	public UserCountInfoDto getUserCountInfo(String userId) {
		UserInfo userInfo = getUserInfoByUserId(userId);
		Integer fansCount = userFocusMapper.selectFansCount(userId);
		Integer focusCount = userFocusMapper.selectFocusCount(userId);

		UserCountInfoDto countInfoDto = new UserCountInfoDto();

		countInfoDto.setFansCount(fansCount);
		countInfoDto.setFocusCount(focusCount);
		countInfoDto.setCurrentCoinCount(userInfo.getCurrentCoinCount());
		return countInfoDto;
	}

	@Override
	public UserInfo getUserDetailInfo(String currentUserId, String userId) {
		UserInfo userInfo = getUserInfoByUserId(userId);//通过userid获取浏览页面的页面主人的信息
		if (null == userInfo) {
			throw new BusinessException(ResponseCodeEnum.CODE_404);
		}
		//查播放量和喜欢数
		CountInfoDto countInfoDto = videoInfoMapper.selectSumCountInfo(userId);
		CopyTools.copyProperties(countInfoDto, userInfo);
		//TODO 粉丝相关
		Integer fansCount = userFocusMapper.selectFansCount(userId);
		Integer focusCount = userFocusMapper.selectFocusCount(userId);
		userInfo.setFansCount(fansCount);
		userInfo.setFocusCount(focusCount);
		// 如果当前调用者未登录（currentUserId 为空），默认返回“未关注”
		if (currentUserId == null) {
			userInfo.setHaveFocus(false);
		} else {
			// 已登录：去关系表查是否关注过目标用户
			UserFocus userFocus = (UserFocus) userFocusMapper.selectByUserIdAndFocusUserId(currentUserId, userId);
			// 查得到记录 → 已关注；查不到 → 未关注
			userInfo.setHaveFocus(userFocus == null ? false : true);
		}
		return userInfo;
	}

	@Override
	@Transactional // 事务：要么全部成功，要么全部回滚
	public void updateUserInfo(UserInfo userInfo, TokenUserInfoDto tokenUserInfoDto) {
		// 查库获取当前用户完整信息（含硬币、头像、昵称等）
		UserInfo dbInfo = this.userInfoMapper.selectByUserId(userInfo.getUserId());

		// 若昵称要改且硬币不足，直接抛异常阻断
		if (!dbInfo.getNickName().equals(userInfo.getNickName()) && dbInfo.getCurrentCoinCount() < Constants.UPDATE_NICK_NAME_COIN) {
			throw new BusinessException("硬币不足，无法修改昵称");
		}

		// 真正扣硬币（乐观锁，返回0表示余额变动失败）
		if (!dbInfo.getNickName().equals(userInfo.getNickName())) {
			Integer count = this.userInfoMapper.updateCoinCountInfo(userInfo.getUserId(), -Constants.UPDATE_NICK_NAME_COIN);
			if (count == 0) {
				throw new BusinessException("硬币不足，无法修改昵称");
			}
		}

		// 更新数据库用户资料（除硬币外其余字段）
		this.userInfoMapper.updateByUserId(userInfo, userInfo.getUserId());

		// 以下逻辑：若头像或昵称有变，同步刷新 Redis 中的 token 信息，保证前端实时一致
		Boolean updateTokenInfo = false;
		if (!userInfo.getAvatar().equals(tokenUserInfoDto.getAvatar())) {
			tokenUserInfoDto.setAvatar(userInfo.getAvatar());
			updateTokenInfo = true;
		}
		if (!tokenUserInfoDto.getNickName().equals(userInfo.getNickName())) {
			tokenUserInfoDto.setNickName(userInfo.getNickName());
			updateTokenInfo = true;
		}
		if (updateTokenInfo) {
			redisComponent.updateTokenInfo(tokenUserInfoDto); // 把最新头像、昵称写回 Redis
		}
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<UserInfo> findListByPage(UserInfoQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<UserInfo> list = this.findListByParam(param);
		PaginationResultVO<UserInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(UserInfo bean) {
		return this.userInfoMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<UserInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userInfoMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<UserInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userInfoMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(UserInfo bean, UserInfoQuery param) {
		StringTools.checkParam(param);
		return this.userInfoMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(UserInfoQuery param) {
		StringTools.checkParam(param);
		return this.userInfoMapper.deleteByParam(param);
	}

	/**
	 * 根据UserId获取对象
	 */
	@Override
	public UserInfo getUserInfoByUserId(String userId) {
		return this.userInfoMapper.selectByUserId(userId);
	}

	/**
	 * 根据UserId修改
	 */
	@Override
	public Integer updateUserInfoByUserId(UserInfo bean, String userId) {
		return this.userInfoMapper.updateByUserId(bean, userId);
	}

	/**
	 * 根据UserId删除
	 */
	@Override
	public Integer deleteUserInfoByUserId(String userId) {
		return this.userInfoMapper.deleteByUserId(userId);
	}

	/**
	 * 根据Email获取对象
	 */
	@Override
	public UserInfo getUserInfoByEmail(String email) {
		return this.userInfoMapper.selectByEmail(email);
	}

	/**
	 * 根据Email修改
	 */
	@Override
	public Integer updateUserInfoByEmail(UserInfo bean, String email) {
		return this.userInfoMapper.updateByEmail(bean, email);
	}

	/**
	 * 根据Email删除
	 */
	@Override
	public Integer deleteUserInfoByEmail(String email) {
		return this.userInfoMapper.deleteByEmail(email);
	}

	/**
	 * 根据NickName获取对象
	 */
	@Override
	public UserInfo getUserInfoByNickName(String nickName) {
		return this.userInfoMapper.selectByNickName(nickName);
	}

	/**
	 * 根据NickName修改
	 */
	@Override
	public Integer updateUserInfoByNickName(UserInfo bean, String nickName) {
		return this.userInfoMapper.updateByNickName(bean, nickName);
	}

	@Override
	public void register(String email, String nickName, String registerPassword) {
/*UserInfo userInfo = this.userInfoMapper.selectByEmail(email);
if(userInfo==null){
	UserInfo nickNameUser=this.userInfoMapper.selectByNickName(nickName);
	if(nickNameUser==null){
		userInfo=new UserInfo();
		String userId=StringTools.getRandomNumber(10);
		userInfo.setUserId(userId);
		userInfo.setNickName(nickName);
		userInfo.setPassword(StringTools.encodeByMd5(registerPassword));
		userInfo.setLoginTime(new Date());
		userInfo.setSex(UserSexEnum.SECRECY.getType());
		userInfo.setTheme(1);
		this.userInfoMapper.insert(userInfo);
		return true;
	}
	else{return false;}
}else{return false;}*/
		UserInfo userInfo = this.userInfoMapper.selectByEmail(email);
		if(userInfo!=null){
			throw  new BusinessException("邮箱账号已存在");
		}
		UserInfo nickNameUser=this.userInfoMapper.selectByNickName(nickName);
		if(nickNameUser!=null){
			throw  new BusinessException("昵称已存在");
		}
		userInfo=new UserInfo();
		String userId=StringTools.getRandomNumber(10);
		userInfo.setEmail(email);
		userInfo.setUserId(userId);
		userInfo.setNickName(nickName);
		userInfo.setPassword(StringTools.encodeByMd5(registerPassword));
		userInfo.setLoginTime(new Date());
		userInfo.setSex(UserSexEnum.SECRECY.getType());
		userInfo.setTheme(1);
		//todo 初始化用户硬币
		userInfo.setCurrentCoinCount(10);
		userInfo.setTotalCoinCount(10);
		this.userInfoMapper.insert(userInfo);
	}

	@Override
	public TokenUserInfoDto login(String email, String password, String ip) {
		// 1. 根据邮箱查询用户信息
		UserInfo userInfo = this.userInfoMapper.selectByEmail(email);

		// 2. 校验用户是否存在，且密码是否正确
		// 如果 userInfo 为空，说明邮箱不存在；
		// 如果密码不匹配，说明用户输入的密码错误；
		// 两种情况都抛出业务异常
		if (userInfo == null || !userInfo.getPassword().equals(password)) {
			throw new BusinessException("账户或密码错误");
		}

		// 3. 构造一个只包含需要更新字段的 UserInfo 对象
		UserInfo updateInfo = new UserInfo();
		updateInfo.setLoginTime(new Date()); // 设置最新登录时间为当前时间
		updateInfo.setLastLoginIp(ip);       // 设置本次登录的 IP 地址

		// 4. 调用数据层方法，按 userId 更新用户的登录信息
		// （只更新传入对象里的非空字段）
		this.userInfoMapper.updateByUserId(updateInfo, userInfo.getUserId());
		TokenUserInfoDto tokenUserInfoDto= CopyTools.copy(userInfo, TokenUserInfoDto.class);
		redisComponent.saveTokenInfo(tokenUserInfoDto);
		return tokenUserInfoDto;
	}


	/**
	 * 根据NickName删除
	 */
	@Override
	public Integer deleteUserInfoByNickName(String nickName) {
		return this.userInfoMapper.deleteByNickName(nickName);
	}
}
