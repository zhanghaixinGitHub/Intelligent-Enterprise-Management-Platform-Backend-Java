package com.company.workflow.handler;
import com.company.workflow.common.exception.BusinessException;
import com.company.workflow.common.model.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
/**
 * ????????
 *
 * <p>????????????????? Flowable????????????
 * ???????????????????????</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBusinessException(BusinessException exception) {
        log.warn("GlobalExceptionHandler.handleBusinessException   >>>   ?????code={}, message={}", exception.getCode(), exception.getMessage());
        return ApiResponse.failure(exception.getCode(), exception.getMessage());
    }
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception exception) {
        log.error("GlobalExceptionHandler.handleException   >>>   ????", exception);
        return ApiResponse.failure("SYSTEM_ERROR", "??????????");
    }
}
