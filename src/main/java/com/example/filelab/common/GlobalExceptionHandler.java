package com.example.filelab.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

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
}