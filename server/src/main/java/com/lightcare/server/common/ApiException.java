package com.lightcare.server.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.OK)
public class ApiException extends RuntimeException {

    public final ApiError error;

    public ApiException(ApiError error) {
        super(error.message);
        this.error = error;
    }

    public ApiException(ApiError error, String message) {
        super(message);
        this.error = error;
    }
}
