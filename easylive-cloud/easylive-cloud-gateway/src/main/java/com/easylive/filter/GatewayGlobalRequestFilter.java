package com.easylive.filter;

import com.easylive.entity.enums.ResponseCodeEnum;
import com.easylive.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
@Component
@Slf4j
public class GatewayGlobalRequestFilter implements GlobalFilter, Ordered {
    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String rawpath = exchange.getRequest().getURI().getRawPath();
        log.info("请求的路径是{}",rawpath);
        if(rawpath.indexOf("innerApi")!=-1){
            throw new BusinessException(ResponseCodeEnum.CODE_404);
        }
        return chain.filter(exchange);

    }
}
