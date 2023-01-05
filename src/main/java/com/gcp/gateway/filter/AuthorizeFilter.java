package com.gcp.gateway.filter;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gcp.gateway.config.RedisCache;
import com.gcp.gateway.response.CommonException;
import com.gcp.gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.net.URI;
import java.util.List;

/**
 * 权限校验过滤器
 * @author Admin
 */
@Order(1)
@Component
@Slf4j
public class AuthorizeFilter implements WebFilter {

    @Resource
    private RedisCache redisCache;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        URI uri = exchange.getRequest().getURI();
        log.info("开始校验路由：{}",uri.getPath());
        if(uri.getPath().contains("login")){
            return chain.filter(exchange);
        }
        String token = exchange.getRequest().getHeaders().getFirst("token");
        log.info("接收的token为：{}",token);
        if(!StringUtils.hasText(token)){
            log.error("token为空");
            throw new CommonException("权限不足，请重新登录");
        }
        String userId;
        // 解析token
        try {
            Claims claims = JwtUtil.parseJWT(token);
            userId = claims.getSubject();
        } catch (Exception e) {
            log.error("解析token失败");
            throw new CommonException("解析token失败");
        }
        // 从redis中获取用户信息
        String redisKey = "login:" + userId;
        redisCache.getCacheObject(redisKey);
        JSONObject jsonObject = redisCache.getCacheObject(redisKey);
        if (jsonObject.isEmpty()) {
            log.error("用户信息获取失败");
            throw new CommonException("账户过期，请重新登录");
        }
        String roleId = exchange.getRequest().getHeaders().getFirst("role_id");
        List<String> list = JSONArray.parseArray(jsonObject.get("permissions").toString(),String.class);
        // 校验是否有该角色
        if (!list.contains(roleId)) {
            log.error("角色不匹配");
            throw new CommonException("权限不足");
        }
        String url = uri.getPath();
        String realUrl;
        realUrl = url.split("\\/")[1];
        String role = redisCache.getCacheObject("role:role_" + roleId);
        // 校验角色是否存在该路径
        if (!role.contains(url.split(realUrl)[1])) {
            log.error("该接口路径无权限");
            throw new CommonException("权限不足!!");
        }
        return chain.filter(exchange);
    }
}
