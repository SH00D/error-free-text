package com.example.errorfreetext.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, 40001),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, 40002),
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, 40003),
    TASK_NOT_FOUND(HttpStatus.NOT_FOUND, 40401),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, 40402),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, 40501),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, 41501),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 50001);

    private final HttpStatus status;
    private final int code;

    ErrorCode(HttpStatus status, int code) {
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public int getCode() {
        return code;
    }
}
