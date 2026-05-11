package com.company.workflow.common.exception;
/**
 * 统一业务异常。
 *
 * <p>业务异常与系统异常分离，便于：</p>
 * <ul>
 *     <li>对外返回可理解错误信息</li>
 *     <li>对内记录完整日志上下文</li>
 *     <li>后续接入统一错误码体系</li>
 * </ul>
 */
public class BusinessException extends RuntimeException {
    private final String code;
    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }
    public String getCode() {
        return code;
    }
}
