package com.company.workflow.handler;
import com.company.workflow.common.exception.BusinessException;
import com.company.workflow.common.model.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
/**
 * 全局异常处理器。
 *
 * <p>统一把参数校验异常、业务异常、系统异常转换成稳定响应，避免 Flowable 接口在异常时返回无结构 HTML 或默认堆栈信息。</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        String message = fieldError == null ? "请求参数校验失败" : fieldError.getDefaultMessage();
        log.warn("GlobalExceptionHandler.handleMethodArgumentNotValidException   >>>   请求参数校验失败，message={}", message);
        return ApiResponse.failure("VALIDATION_ERROR", message);
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBusinessException(BusinessException exception) {
        log.warn("GlobalExceptionHandler.handleBusinessException   >>>   捕获业务异常，code={}, message={}", exception.getCode(), exception.getMessage());
        return ApiResponse.failure(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception exception) {
        log.error("GlobalExceptionHandler.handleException   >>>   捕获系统异常", exception);
        return ApiResponse.failure("SYSTEM_ERROR", "系统开小差了，请稍后再试");
    }
}
