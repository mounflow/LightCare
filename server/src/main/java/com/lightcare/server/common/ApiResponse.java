package com.lightcare.server.common;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record ApiResponse<T>(int code, String message, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(ApiError.OK.code, ApiError.OK.message, data);
    }

    public static <T> ApiResponse<T> fail(ApiError error) {
        return new ApiResponse<>(error.code, error.message, null);
    }
}
