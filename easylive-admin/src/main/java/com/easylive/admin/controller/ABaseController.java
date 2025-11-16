package com.easylive.admin.controller;
import com.easylive.component.RedisComponent;
import com.easylive.entity.dto.TokenUserInfoDto;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.enums.ResponseCodeEnum;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.exception.BusinessException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class ABaseController {
@Resource
private RedisComponent redisComponent;
    protected static final String STATUC_SUCCESS = "success";

    protected static final String STATUC_ERROR = "error";

    protected <T> ResponseVO getSuccessResponseVO(T t) {
        ResponseVO<T> responseVO = new ResponseVO<>();
        responseVO.setStatus(STATUC_SUCCESS);
        responseVO.setCode(ResponseCodeEnum.CODE_200.getCode());
        responseVO.setInfo(ResponseCodeEnum.CODE_200.getMsg());
        responseVO.setData(t);
        return responseVO;
    }
    protected String getIpAddr() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String ip = request.getHeader("x-forwarded-for");
        if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
            // 多次反向代理后会有多个ip值，第一个ip才是真实ip
            if (ip.indexOf(",") != -1) {
                ip = ip.split(",")[0];
            }
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
    protected <T> ResponseVO getBusinessErrorResponseVO(BusinessException e, T t) {
        ResponseVO vo = new ResponseVO();
        vo.setStatus(STATUC_ERROR);
        if (e.getCode() == null) {
            vo.setCode(ResponseCodeEnum.CODE_600.getCode());
        } else {
            vo.setCode(e.getCode());
        }
        vo.setInfo(e.getMessage());
        vo.setData(t);
        return vo;
    }

    protected <T> ResponseVO getServerErrorResponseVO(T t) {
        ResponseVO vo = new ResponseVO();
        vo.setStatus(STATUC_ERROR);
        vo.setCode(ResponseCodeEnum.CODE_500.getCode());
        vo.setInfo(ResponseCodeEnum.CODE_500.getMsg());
        vo.setData(t);
        return vo;
    }
    /**
     * 将登录生成的 token 保存到浏览器 Cookie 中
     * @param response HttpServletResponse，用于向客户端写入 Cookie
     * @param token    用户登录后生成的 token
     */
    protected void saveToken2Cookie(HttpServletResponse response, String token) {
        // 1. 创建一个 Cookie，键为 Constants.TOKEN_WEB，值为 token
        //    注意：这里的 TOKEN_WEB 应该是一个常量字符串（比如 "token:web"）
        Cookie cookie = new Cookie(Constants.TOKEN_ADMIN, token);

        // 2. 设置 Cookie 的过期时间（单位：秒）
        //    Constants.REDIS_KEY_EXPIRES_ONE_DAY 单位是毫秒
        //    因此这里先乘以 7，再除以 1000，得到“7 天”的秒数
        cookie.setMaxAge(-1);

        // 3. 设置 Cookie 的作用路径为整个网站（"/"）
        //    这样无论访问哪个路径，浏览器都会携带该 Cookie
        cookie.setPath("/");

        // 4. 将 Cookie 添加到响应中，发送给客户端浏览器
        response.addCookie(cookie);
    }
protected TokenUserInfoDto getTokenUserInfoDto(){
    HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    String token =request.getHeader(Constants.TOKEN_WEB);
    return redisComponent.getTokenInfo(token);
}
    protected void cleanCookie(HttpServletResponse response) {

        // 从 RequestContextHolder 中获取当前 HTTP 请求对象
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

        // 获取当前请求中的所有 Cookie
        Cookie[] cookies = request.getCookies();
if(cookies==null){
    return;
}
        // 遍历所有的 Cookie
        for (Cookie cookie : cookies) {
            if(cookie.getName().equals(Constants.TOKEN_ADMIN)){
            // 清除与验证码相关的 Redis 信息
            redisComponent.cleanToken(cookie.getValue());

            // 设置当前 Cookie 的最大生存时间为 0，这将使得 Cookie 立即过期
            cookie.setMaxAge(0);

            // 设置 Cookie 的路径为根路径，确保 Cookie 在整个网站范围内都有效
            cookie.setPath("/");

            // 将修改后的 Cookie 添加到响应中，告知浏览器删除该 Cookie
            response.addCookie(cookie);

            // 在这里使用 break，表示只删除第一个匹配的 Cookie，若需要删除所有 Cookie，可以移除 break
            break;
        }
    }}

}
