package com.gcp.getway.config;

import com.gcp.getway.response.CommonException;
import com.gcp.getway.response.ResponseModels;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Admin
 */
@RestControllerAdvice
@Slf4j
public class ExceptionAdv {

    private <T> ResponseEntity<T> buildResponse(T t, HttpServletRequest request) {
        return ResponseEntity.ok(t);
    }

    @ExceptionHandler({CommonException.class})
    public Object handleBusinessException(HttpServletRequest request, HttpServletResponse response, CommonException e) throws IOException {
        log.error("进入全局异常处理器");
        return this.buildResponse(ResponseModels.commonException(e), request);
    }

}
