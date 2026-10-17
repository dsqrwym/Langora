package org.dsqrwym.langora.infrastructure.web;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public record ApiResponse<T>(int status, String code, String message, String traceId, T data) {
    public static final String TRACE_ID_KEY = "traceId";
    public static final String OK_CODE = "OK";
    public static final String CREATED_CODE = "CREATED";
    public static final String ACCEPTED_CODE = "ACCEPTED";
    public static final String NO_CONTENT_CODE = "NO_CONTENT";
    public static final String MSG_SUCCESS = "Success";

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(HttpStatus.OK.value(), OK_CODE, MSG_SUCCESS, MDC.get(TRACE_ID_KEY), data);
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(HttpStatus.CREATED.value(), CREATED_CODE, MSG_SUCCESS, MDC.get(TRACE_ID_KEY), data);
    }

    public static <T> ApiResponse<T> error(HttpStatusCode status, ErrorCode code, T data) {
        return new ApiResponse<>(status.value(), code.name(), code.defaultMessage(), MDC.get(TRACE_ID_KEY), data);
    }

    public static <T> ApiResponse<T> of(int status, T data) {
        if (status >= 200 && status < 300) {
            String code = switch (status) {
                case 201 -> CREATED_CODE;
                case 202 -> ACCEPTED_CODE;
                case 204 -> NO_CONTENT_CODE;
                default -> OK_CODE;
            };
            return new ApiResponse<>(status, code, MSG_SUCCESS, MDC.get(TRACE_ID_KEY), data);
        }

        ErrorCode errorCode = ErrorCode.fromHttpStatus(HttpStatusCode.valueOf(status));
        return new ApiResponse<>(status, errorCode.name(), errorCode.defaultMessage(), MDC.get(TRACE_ID_KEY), data);
    }
}
