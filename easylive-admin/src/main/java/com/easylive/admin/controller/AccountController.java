package com.easylive.admin.controller; // 控制器包

import java.security.SecureRandom;
import java.util.HashMap; // Map 实现
import java.util.Map;     // Map 接口

import com.easylive.component.RedisComponent; // 自研 Redis 组件
import com.easylive.entity.config.AppConfig;
import com.easylive.entity.dto.TokenUserInfoDto;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.vo.ResponseVO; // 统一返回体
import com.easylive.exception.BusinessException;
import com.easylive.redis.RedisUtils;
import com.easylive.service.UserInfoService; // 用户服务
import com.easylive.utils.StringTools;
import com.wf.captcha.ArithmeticCaptcha; // 算术验证码
import org.apache.ibatis.annotations.Param; // MyBatis Param（此处不建议用于校验）
import org.springframework.boot.web.servlet.server.Session;
import org.springframework.validation.annotation.Validated; // 参数校验启用
import org.springframework.web.bind.annotation.RequestMapping; // 路由映射
import org.springframework.web.bind.annotation.RestController; // REST 控制器

import javax.annotation.Resource; // 注入
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.Email; // 邮箱校验
import javax.validation.constraints.NotEmpty; // 非空
import javax.validation.constraints.Size; // 长度限制

/**
 * 用户信息 Controller
 */
@RestController("accountController")
@RequestMapping("/account")
@Validated // 控制器路由前缀
public class AccountController extends ABaseController { // 继承基础控制器
@Resource
private AppConfig appConfig;
	@Resource
	private RedisUtils redisUtils;

	@Resource
	private UserInfoService userInfoService; // 注入用户服务

	@Resource
	private RedisComponent redisComponent; // 注入 Redis 组件

	@RequestMapping("/checkCode") // 生成验证码
	public ResponseVO checkCode() { // 返回统一响应
		ArithmeticCaptcha captcha = new ArithmeticCaptcha(100, 47); // 创建 100x47 验证码
		String code = captcha.text(); // 获取验证码答案
		String checkCodeKey = redisComponent.saveCheckCode(code); // 保存答案到 Redis 返回 key
		String checkCodeBase64 = captcha.toBase64(); // 转 Base64 图片
		Map<String, String> result = new HashMap<>(); // 组装返回数据
		result.put("checkCode", checkCodeBase64); // 放入图片
		result.put("checkCodeKey", checkCodeKey); // 放入校验用 key
		return getSuccessResponseVO(result); // 返回成功响应
	}


	@RequestMapping("/login") // 管理端登录
	public ResponseVO login(HttpServletResponse response, HttpServletRequest request, @NotEmpty String account,
							@NotEmpty String password,
							@NotEmpty String checkCodeKey,
							@NotEmpty String checkCode
							) {
		try{
			if(!checkCode.equalsIgnoreCase(redisComponent.getCheckCode(checkCodeKey))){
				throw new BusinessException("图形验证码不正确");
			}
			if(!account.equals(appConfig.getAdminAccount())||!password.equals(StringTools.encodeByMd5(appConfig.getAdminPassword())))
			{throw new BusinessException("账号或密码错误");};
			String token=redisComponent.saveTokenInfo4Admin(account);
			saveToken2Cookie(response,token);
			return getSuccessResponseVO(account);

		}finally {
			redisUtils.delete(Constants.REDIS_KEY_CHECK_CODE + checkCodeKey);
			Cookie[] cookies=request.getCookies();
			String token=null;
			for(Cookie cookie : cookies){
				if(cookie.getName().equals(Constants.TOKEN_ADMIN)){
					token =cookie.getValue();
				}
			}
			if(!StringTools.isEmpty(token)){
				 redisComponent.cleanToken4Admin(token);
			}
		}

	}



	@RequestMapping("/logout") // 用户退出
	public ResponseVO logout(HttpServletResponse response
	) {
		cleanCookie(response);
		return getSuccessResponseVO(null);
	}
	@RequestMapping (value = "/autoLogin")
	public ResponseVO autoLogin(HttpServletResponse response) {
		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
		if (tokenUserInfoDto == null) {
			return getSuccessResponseVO(null);
		}
		if (tokenUserInfoDto.getExpireAt() - System.currentTimeMillis() < Constants.REDIS_KEY_EXPIRES_ONE_DAY) {
			redisComponent.saveTokenInfo(tokenUserInfoDto);
			saveToken2Cookie(response, tokenUserInfoDto.getToken());
		}
		return getSuccessResponseVO(tokenUserInfoDto);
	}
}
