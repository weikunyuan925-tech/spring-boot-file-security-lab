package com.example.filelab.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理上传文件超过大小限制的异常。
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException e) {

        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Result.error(413, "文件大小超过限制"));
    }

    /**
     * 处理其他 multipart 请求异常。
     * 某些情况下，超限异常可能被包装在 MultipartException 中。
     */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<?> handleMultipartException(
            MultipartException e) {

        if (hasSizeLimitCause(e)) {
            return ResponseEntity
                    .status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(Result.error(413, "文件大小超过限制"));
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Result.error(400, "文件上传请求无效"));
    }

    /**
     * 检查异常及其原因链中是否包含大小超限异常。
     */
    private boolean hasSizeLimitCause(Throwable e) {

        Throwable current = e;

        while (current != null) {
            if (current instanceof MaxUploadSizeExceededException) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result> handleValidationException(
            MethodArgumentNotValidException e) {

        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getDefaultMessage())
                .filter(msg -> msg != null && !msg.isBlank())
                .findFirst()
                .orElse("请求参数校验失败");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Result.error(400, message));
    }

    // 一个不存在的地址返回
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result> handleNoResourceFound(
            NoResourceFoundException e) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Result.error(404, "请求的资源不存在"));
    }

   // 兜底的异常反馈
    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result> handleGeneralException(Exception e) {
        // 服务端记录完整异常，供排查问题使用
        log.error("发生未处理的服务端异常", e);

        // 客户端只收到通用错误信息，不暴露内部细节
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(500, "服务器内部错误，请稍后重试"));
    }
}