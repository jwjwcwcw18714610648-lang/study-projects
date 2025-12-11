package com.easylive.filter;

import com.easylive.entity.constants.Constants;
import com.easylive.entity.enums.ResponseCodeEnum;
import com.easylive.exception.BusinessException;
import com.easylive.utils.StringTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AdminFilter extends AbstractGatewayFilterFactory {

    private final static String URL_ACCOUNT = "/account";
    private final static String URL_FILE = "/file";

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            if (request.getURI().getRawPath().contains(URL_ACCOUNT)) {
                return chain.filter(exchange);
            }
            String token = getToken(request);

            //获取文件直接从cookie中获取token
            if (request.getURI().getRawPath().contains(URL_FILE)) {
                token = getTokenFromCookie(request);
            }

            if (StringTools.isEmpty(token)) {
                throw new BusinessException(ResponseCodeEnum.CODE_901);
            }
            return chain.filter(exchange);
        };
    }

    private String getToken(ServerHttpRequest request) {
        return request.getCookies().getFirst(Constants.TOKEN_WEB).getValue();
    }

    private String getTokenFromCookie(ServerHttpRequest request) {
        return request.getCookies().getFirst(Constants.TOKEN_WEB).getValue();
    }
}
