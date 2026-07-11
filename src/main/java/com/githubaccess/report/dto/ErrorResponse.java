package com.githubaccess.report.dto;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        String errorCode,
        String message,
        String path,
        Instant timestamp,
        List<FieldError> fieldErrors
) {

    public static ErrorResponse of(String code, String message, String path) {
        return new ErrorResponse(code, message, path, Instant.now(), List.of());
    }

    public static ErrorResponse ofValidation(String code, List<FieldError> errors, String path) {
        return new ErrorResponse(code, "Validation failed", path, Instant.now(), errors);
    }

    public record FieldError(String field, String message) {
    }
}
