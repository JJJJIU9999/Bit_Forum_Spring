package com.bitforum.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.bitforum.common.Result;

@RestControllerAdvice
public class GlobalExceptionHandler {
    // 服务端日志记录完整异常；客户端只返回安全、稳定的提示
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 参数校验失败属于用户输入错误，可以把字段校验提示返回给前端
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<String>> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldError().getDefaultMessage();
        return response(HttpStatus.BAD_REQUEST, msg);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<String>> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        return response(HttpStatus.BAD_REQUEST, "上传文件过大，头像不能超过 2MB，文章封面不能超过 5MB");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Result<String>> handleResponseStatus(ResponseStatusException e) {
        String message = e.getReason() == null ? "请求处理失败" : e.getReason();
        return ResponseEntity.status(e.getStatusCode())
                .body(Result.fail(e.getStatusCode().value(), message));
    }

    // 兜底处理未知异常：日志保留细节，响应不暴露 e.getMessage()
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<String>> handleException(Exception e) {
        log.error("服务器发生未处理异常",e);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误，请稍后尝试");
    }

    // 请求路径不存在时，返回固定提示，不需要暴露内部路由细节
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<Result<String>> handle404(Exception e) {
        return response(HttpStatus.NOT_FOUND, "接口不存在，请检查请求路径");
    }

    private ResponseEntity<Result<String>> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Result.fail(status.value(), message));
    }
}
