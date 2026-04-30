package com.yupi.yuaicodemother.exception;

import com.yupi.yuaicodemother.common.BaseResponse;
import com.yupi.yuaicodemother.common.ResultUtils;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Hidden
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e, HttpServletResponse response) {
        // SSE 流已开始（响应已提交）或 Content-Type 为 event-stream，无法写 JSON，静默返回 null
        if (isSseResponse(response)) {
            log.warn("BusinessException in SSE stream (ignored): {}", e.getMessage());
            return null;
        }
        log.error("BusinessException", e);
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e, HttpServletResponse response) {
        // 客户端断开连接（closed / connection reset）属于正常行为，静默忽略
        String msg = e.getMessage();
        if (msg != null && (msg.contains("closed") || msg.contains("Connection reset")
                || msg.contains("Broken pipe") || msg.contains("cancel"))) {
            log.debug("客户端断开连接（SSE 正常行为），忽略异常: {}", msg);
            return null;
        }
        // SSE 流已提交，无法写 JSON 响应
        if (isSseResponse(response)) {
            log.warn("RuntimeException in SSE stream (ignored): {}", msg);
            return null;
        }
        log.error("RuntimeException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }

    /**
     * 判断是否为 SSE 响应（响应已提交 或 Content-Type 为 text/event-stream）
     */
    private boolean isSseResponse(HttpServletResponse response) {
        if (response.isCommitted()) {
            return true;
        }
        String contentType = response.getContentType();
        return contentType != null && contentType.contains("text/event-stream");
    }
}