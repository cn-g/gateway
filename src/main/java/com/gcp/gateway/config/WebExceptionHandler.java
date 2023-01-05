package com.gcp.gateway.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcp.gateway.response.CommonException;
import com.gcp.gateway.response.ResponseCode;
import com.gcp.gateway.response.ResponseModelDto;
import com.gcp.gateway.response.ResponseModels;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

/**
 * filter异常响应
 * @author Admin
 */
@Slf4j
@Order(-1)
@Component
public class WebExceptionHandler implements ErrorWebExceptionHandler {

    @Resource
    ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        response.setStatusCode(HttpStatus.OK);

        ResponseModelDto res;
        if (ex instanceof CommonException) {
            log.debug("接口调用失败,url={},message={}", exchange.getRequest().getURI().toString(), ex.getMessage());
            res = ResponseModels.commonException((CommonException)ex);
        } else {
            log.error("接口调用失败,url={},message={}", exchange.getRequest().getURI().toString(), ex.getMessage());
            res = ResponseModels.commonException().message(ResponseCode.CommonException + "[G]");
        }
        HttpHeaders headers = response.getHeaders();
        headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "*");
        headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "*");
        headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        headers.set(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "*");
        headers.set(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
        headers.setContentType(MediaType.APPLICATION_JSON);
        return response.writeWith(Mono.fromSupplier(() -> {
            DataBufferFactory bufferFactory = response.bufferFactory();

            try {
                return bufferFactory.wrap(objectMapper.writeValueAsBytes(res));
            } catch (JsonProcessingException e) {
                log.warn("Error writing response", ex);
                return bufferFactory.wrap(new byte[0]);
            }
        }));
    }
}
