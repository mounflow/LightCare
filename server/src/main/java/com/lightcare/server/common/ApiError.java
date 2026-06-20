package com.lightcare.server.common;

/**
 * 全局统一错误码。保持简短、与业务解耦。
 */
public enum ApiError {
    OK(0, "ok"),
    BAD_REQUEST(40000, "请求参数有误"),
    UNAUTHORIZED(40100, "请先登录"),
    FORBIDDEN(40300, "无权限"),
    NOT_FOUND(40400, "资源不存在"),
    SMS_TOO_FREQUENT(42901, "短信发送过于频繁"),
    SMS_CODE_INVALID(42902, "验证码无效或已过期"),
    SERVICE_UNAVAILABLE(50300, "服务暂不可用"),
    UPSTREAM_ERROR(50200, "上游服务返回异常"),
    INTERNAL(50000, "服务器开小差了，稍后再试");

    public final int code;
    public final String message;

    ApiError(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
