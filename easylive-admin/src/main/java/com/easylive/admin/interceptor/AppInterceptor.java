package com.easylive.admin.interceptor;

import com.easylive.component.RedisComponent;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.enums.ResponseCodeEnum;
import com.easylive.exception.BusinessException;
import com.easylive.utils.StringTools;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * AppInterceptor —— 全局拦截器
 * 主要职责：在进入控制器方法之前，统一进行登录态校验（基于 token + Redis）。
 */
@Component  // 让 Spring 容器扫描并管理这个拦截器
public class AppInterceptor implements HandlerInterceptor {

    // 不需要校验登录的接口前缀：账号相关（如登录、注册）
    private final static String URL_ACCOUNT = "/account";
    // 文件下载相关接口（token 从 Cookie 中获取）
    private final static String URL_FILE = "/file";

    @Resource
    private RedisComponent redisComponent; // 依赖 Redis 工具类，用于获取登录信息


    /**
     * 进入 Controller 方法前执行
     * 返回 true 表示放行，false 表示拦截
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // handler 为空，不处理，直接拦截
        if (null == handler) {
            return false;
        }

        // 如果不是 HandlerMethod（可能是静态资源请求），直接放行
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        // 如果是账号相关接口（如登录、注册），放行
        if (request.getRequestURI().contains(URL_ACCOUNT)) {
            return true;
        }

        // 默认从请求头中获取 token
        String token = request.getHeader(Constants.TOKEN_ADMIN);

// 如果请求头没有，再尝试从 Cookie 获取
        if (StringTools.isEmpty(token)) {
            token = getTokenFromCookie(request);
        }

        // 如果是文件相关接口（如下载），则从 Cookie 中获取 token
        if (request.getRequestURI().contains(URL_FILE)) {
            token = getTokenFromCookie(request);
        }

        // 如果 token 为空，抛出业务异常（未登录）
        if (StringTools.isEmpty(token)) {
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }

        // 通过 Redis 查询 token 对应的用户登录信息
        Object sessionObj = redisComponent.getLoginInfo4Admin(token);

        // 如果 Redis 中不存在，说明 token 无效或过期，抛出业务异常（未登录）
        if (sessionObj == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }

        // 校验通过，放行
        return true;
    }


    /**
     * 从 Cookie 中提取 token（文件请求使用）
     */
    private String getTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null; // 没有 Cookie
        }
        for (Cookie cookie : cookies) {
            // 遍历查找名为 TOKEN_ADMIN 的 Cookie
            if (cookie.getName().equalsIgnoreCase(Constants.TOKEN_ADMIN)) {
                return cookie.getValue(); // 返回其值
            }
        }
        return null; // 没找到
    }


    /**
     * Controller 方法执行后、视图渲染前调用
     * 本例未做扩展，直接调用父类默认实现
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) throws Exception {
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    /**
     * 整个请求完成后（包括视图渲染）调用
     * 本例未做扩展，直接调用父类默认实现
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) throws Exception {
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
