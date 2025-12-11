package com.easylive.web.controller; // 控制器包

import com.easylive.component.RedisComponent;
import com.easylive.entity.config.AppConfig;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.dto.TokenUserInfoDto;
import com.easylive.entity.po.UserCountInfoDto;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.exception.BusinessException;
import com.easylive.service.UserInfoService;
import com.easylive.utils.StringTools;

import com.easylive.web.annotation.GlobalInterceptor;
import com.wf.captcha.ArithmeticCaptcha;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.HashMap;
import java.util.Map;


/**
 * 用户信息 Controller
 */
@RestController("accountController")
@RequestMapping("/account")
@Validated
@Slf4j
public class AccountController extends ABaseController { // 继承基础控制器

	@Resource
	private UserInfoService userInfoService; // 注入用户服务

	@Resource
	private RedisComponent redisComponent; // 注入 Redis 组件
	private static final Logger logger = LoggerFactory.getLogger(AccountController.class);
	@RequestMapping(value = "/checkCode")
	public ResponseVO checkCode() {
		ArithmeticCaptcha captcha = new ArithmeticCaptcha(100, 42);
		String code = captcha.text();
		String checkCodeKey = redisComponent.saveCheckCode(code);
		String checkCodeBase64 = captcha.toBase64();
		Map<String, String> result = new HashMap<>();
		result.put("checkCode", checkCodeBase64);
		result.put("checkCodeKey", checkCodeKey);
		return getSuccessResponseVO(result);
	}

	@RequestMapping(value = "/register")

	public ResponseVO register(@NotEmpty @Email @Size(max = 150) String email,
							   @NotEmpty @Size(max = 20) String nickName,
							   @NotEmpty @Pattern(regexp =
			Constants.REGEX_PASSWORD) String registerPassword,
							   @NotEmpty String checkCodeKey, @NotEmpty String checkCode) {
		try {
			if (!checkCode.equalsIgnoreCase(redisComponent.getCheckCode(checkCodeKey))) {
				throw new BusinessException("图片验证码不正确");
			}
			userInfoService.register(email, nickName, registerPassword);
			return getSuccessResponseVO(null);
		} finally {
			redisComponent.cleanCheckCode(checkCodeKey);
		}
	}

	@RequestMapping(value = "/login")

	public ResponseVO login(HttpServletRequest request, HttpServletResponse response, @NotEmpty @Email String email, @NotEmpty String password,
							@NotEmpty String checkCodeKey, @NotEmpty String checkCode) {
		try {
			if (!checkCode.equalsIgnoreCase(redisComponent.getCheckCode(checkCodeKey))) {
				throw new BusinessException("图片验证码不正确");
			}
			String ip = getIpAddr();
			TokenUserInfoDto tokenUserInfoDto = userInfoService.login(email, password, ip);
			saveToken2Cookie(response, tokenUserInfoDto.getToken());
			return getSuccessResponseVO(tokenUserInfoDto);
		} finally {
			redisComponent.cleanCheckCode(checkCodeKey);
		}
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

	@RequestMapping(value = "/logout")

	public ResponseVO logout(HttpServletResponse response) {
		cleanCookie(response);
		return getSuccessResponseVO(null);
	}
	@RequestMapping(value = "/getUserCountInfo")
	@GlobalInterceptor(checkLogin = true)
	public ResponseVO getUserCountInfo() {
		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto();
		UserCountInfoDto userCountInfoDto = userInfoService.getUserCountInfo(tokenUserInfoDto.getUserId());
		return getSuccessResponseVO(userCountInfoDto);
	}
}
