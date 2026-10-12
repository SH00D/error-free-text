package com.example.errorfreetext.exception;

import com.example.errorfreetext.dto.ErrorResponse;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.Arrays;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTaskNotFound(TaskNotFoundException e, HttpServletRequest request) {
        return build(ErrorCode.TASK_NOT_FOUND, e.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e,
                                                          HttpServletRequest request) {
        StringBuilder message = new StringBuilder();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            if (message.length() > 0) {
                message.append("; ");
            }
            message.append(error.getField()).append(": ").append(error.getDefaultMessage());
        }
        return build(ErrorCode.VALIDATION_FAILED, message.toString(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e,
                                                           HttpServletRequest request) {
        if (e.getCause() instanceof InvalidFormatException cause && cause.getTargetType().isEnum()) {
            String field = cause.getPath().isEmpty() ? "значение" : cause.getPath().get(0).getFieldName();
            String message = "Недопустимое значение '%s' для поля '%s'. Допустимые значения: %s".formatted(
                    cause.getValue(), field, Arrays.toString(cause.getTargetType().getEnumConstants()));
            return build(ErrorCode.VALIDATION_FAILED, message, request);
        }
        return build(ErrorCode.MALFORMED_REQUEST, "Некорректное JSON-тело запроса", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e,
                                                            HttpServletRequest request) {
        String message = "Недопустимое значение '%s' для параметра '%s'".formatted(e.getValue(), e.getName());
        return build(ErrorCode.INVALID_PARAMETER, message, request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException e, HttpServletRequest request) {
        return build(ErrorCode.RESOURCE_NOT_FOUND, "Ресурс не найден", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e,
                                                                HttpServletRequest request) {
        return build(ErrorCode.METHOD_NOT_ALLOWED, e.getMessage(), request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException e,
                                                         HttpServletRequest request) {
        return build(ErrorCode.UNSUPPORTED_MEDIA_TYPE, e.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e, HttpServletRequest request) {
        log.error("Неожиданная ошибка при обработке {} {}", request.getMethod(), request.getRequestURI(), e);
        return build(ErrorCode.INTERNAL_ERROR, "Внутренняя ошибка сервера", request);
    }

    private ResponseEntity<ErrorResponse> build(ErrorCode errorCode, String message, HttpServletRequest request) {
        if (errorCode != ErrorCode.INTERNAL_ERROR) {
            log.warn("Запрос {} {} завершился ошибкой: {}", request.getMethod(), request.getRequestURI(), message);
        }
        ErrorResponse body = new ErrorResponse(message, errorCode.getCode(), LocalDateTime.now(),
                request.getRequestURI());
        return ResponseEntity.status(errorCode.getStatus()).body(body);
    }
}
