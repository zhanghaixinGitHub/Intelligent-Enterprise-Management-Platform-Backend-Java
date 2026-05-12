package com.company.workflow.common.model;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
 * 统一响应体。
 *
 * <p>企业级项目需要把接口成功态、错误码、提示信息统一收敛，避免：</p>
 * <ul>
 *     <li>不同控制器返回结构不一致，导致前端和 Python 平台适配成本升高</li>
 *     <li>后续新增 traceId、分页元数据、审计字段时需要逐个接口重构</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String code;
    private String message;
    private T data;
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "SUCCESS", "操作成功", data);
    }
    public static <T> ApiResponse<T> failure(String code, String message) {
        return new ApiResponse<>(false, code, message, null);
    }
}
