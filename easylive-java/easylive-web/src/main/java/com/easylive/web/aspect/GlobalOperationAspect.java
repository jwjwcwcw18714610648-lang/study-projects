package com.easylive.web.aspect;

import com.easylive.entity.constants.Constants;
import com.easylive.entity.dto.TokenUserInfoDto;
import com.easylive.entity.enums.ResponseCodeEnum;
import com.easylive.exception.BusinessException;
import com.easylive.redis.RedisUtils;
import com.easylive.web.annotation.GlobalInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

/**
 * 全局操作切面：用于拦截带有 @GlobalInterceptor 注解的方法，实现如登录校验等功能
 */
@Component("operationAspect")
@Aspect // 声明该类为AOP切面
@Slf4j
public class GlobalOperationAspect {

    // 注入RedisUtils，用于从Redis中获取用户登录信息
    @Resource
    private RedisUtils redisUtils;

    /**
     * 前置通知：当方法被 @GlobalInterceptor 注解标记时，优先执行此方法
     *
     * 该方法主要实现两个功能：
     * 1. 通过反射拿到目标方法及其注解；
     * 2. 如果注解上标注了 checkLogin=true，则强制校验当前用户是否已登录（凭token）。
     */
    @Before("@annotation(com.easylive.web.annotation.GlobalInterceptor)")
    public void interceptoDo(JoinPoint point) {
        // 获取被拦截方法的Method对象
        Method method = ((MethodSignature) point.getSignature()).getMethod();
        // 获取方法上的 @GlobalInterceptor 注解
        GlobalInterceptor interceptor = method.getAnnotation(GlobalInterceptor.class);

        // 注解可能不存在，直接返回
        if (interceptor == null) return;

        // 如果注解要求校验登录，则执行登录校验
        if (interceptor.checkLogin()) {
            checkLogin();
        }
    }

    /**
     * 校验用户是否已登录
     *
     * 实现步骤：
     * 1. 获取当前请求对象，从Cookie中提取token；
     * 2. 使用该token去Redis中查找关联的用户信息（TokenUserInfoDto）；
     * 3. 如果未查到，抛出“未登录”业务异常，否则说明已登录。
     */
    private void checkLogin() {
        // 1. 获取当前请求对象
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

        // 2. 从cookie中获取token（TOKEN_WEB）
        String token = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (Constants.TOKEN_WEB.equals(c.getName())) {
                    token = c.getValue();
                    break;
                }
            }
        }

        // 3. 用token拼接Redis key，查Redis中是否有对应的用户登录信息
        TokenUserInfoDto tokenUserInfoDto = (TokenUserInfoDto) redisUtils.get(Constants.REDIS_KEY_TOKEN_WEB + token);

        // 4. 如果查不到，则抛出未登录异常
        if (tokenUserInfoDto == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_901); // CODE_901通常表示“未登录”
        }
    }
}
