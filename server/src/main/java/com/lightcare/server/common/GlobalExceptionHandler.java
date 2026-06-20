package com.lightcare.server.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApi(ApiException ex) {
        return ResponseEntity
            .status(ex.error == ApiError.UNAUTHORIZED ? HttpStatus.UNAUTHORIZED : HttpStatus.OK)
            .body(ApiResponse.fail(ex.error));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAny(Exception ex) {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(new ApiResponse<>(ApiError.INTERNAL.code, ex.getMessage(), null));
    }
}
