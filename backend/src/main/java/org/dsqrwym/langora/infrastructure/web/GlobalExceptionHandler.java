package org.dsqrwym.langora.infrastructure.web;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.dsqrwym.langora.infrastructure.web.dto.ParamErrorDetail;
import org.dsqrwym.langora.infrastructure.web.dto.ValidationErrorDetail;
import org.dsqrwym.langora.infrastructure.web.exception.BusinessException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * Cath all exceptions for Spring MVC built-ins.
     *
     * @param ex      the exception to handle
     * @param headers the headers to be written to the response
     * @param status  the selected response status
     * @param request the current request
     * @return the response entity
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            @NonNull Exception ex, Object body, @NonNull HttpHeaders headers, @NonNull HttpStatusCode status, @NonNull WebRequest request
    ) {
        HttpStatus resolvedStatus = HttpStatus.resolve(status.value());
        String reason = (resolvedStatus != null) ? resolvedStatus.getReasonPhrase() : "Request failed";

        return ResponseEntity.status(resolvedStatus != null ? resolvedStatus : HttpStatus.INTERNAL_SERVER_ERROR).headers(headers).body(ApiResponse.error(status, "HTTP_" + status.value(), reason, null));
    }

    /**
     * Bean Validation on @RequestBody
     * data: {"field": "field_name", "code": "validation_code"}
     *
     * @param ex      the exception to handle
     * @param headers the headers to be written to the response
     * @param status  the selected response status
     * @param request the current request
     * @return the response entity
     */
    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, @NonNull HttpHeaders headers, @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        List<ValidationErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ValidationErrorDetail(error.getField(), String.valueOf(error.getCode()), error.getDefaultMessage()))
                .toList();
        return ResponseEntity.status(status).headers(headers).body(ApiResponse.error(status, "HTTP_" + status.value(), ApiResponse.MSG_VALIDATION_FAILED, details));
    }

    /**
     * RequestParam, PathVariable validation
     *
     * @param ex      the exception to handle
     * @param headers the headers to be written to the response
     * @param status  the selected response status
     * @param request the current request
     * @return the response entity
     */
    @Override
    protected @Nullable ResponseEntity<Object> handleHandlerMethodValidationException(@NonNull HandlerMethodValidationException ex, @NonNull HttpHeaders headers, @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        List<ParamErrorDetail> details = ex.getParameterValidationResults().stream()
                .map(
                        error -> new ParamErrorDetail(String.valueOf(error.getMethodParameter().getParameterName()), "Invalid")
                )
                .toList();
        return ResponseEntity.badRequest().body(
                ApiResponse.error(HttpStatus.BAD_REQUEST, ApiResponse.VALIDATION_ERROR_CODE, ApiResponse.MSG_VALIDATION_FAILED, details)
        );
    }

    /**
     * Method level validation @Validated
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> onConstraintViolation(ConstraintViolationException ex) {
        List<ValidationErrorDetail> details = ex.getConstraintViolations().stream()
                .map(error -> new ValidationErrorDetail(
                        error.getPropertyPath().toString(),
                        error.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName(),
                        error.getMessage()
                )).toList();

        return ResponseEntity.badRequest().body(
                ApiResponse.error(HttpStatus.BAD_REQUEST, ApiResponse.VALIDATION_ERROR_CODE, ApiResponse.MSG_VALIDATION_FAILED, details)
        );
    }

    /**
     * Business exception
     *
     * @param ex the exception to handle
     * @return the response entity
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> onBusinessException(BusinessException ex) {
        return ResponseEntity.status(ex.status).body(
                ApiResponse.error(ex.status, ex.code.name(), ex.msg, null)
        );
    }

    /**
     * Access denied throw from controllers
     *
     * @param ex the exception to handle
     * @return the response entity
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> onAccessDeniedException(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ApiResponse.error(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN.name(), ApiResponse.MSG_FORBIDDEN, null)
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> onUnknownException(Exception ex) {
        logger.error("Unknown exception", ex);
        return ResponseEntity.internalServerError().body(
                ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR.name(), ApiResponse.MSG_INTERNAL_ERROR, null)
        );
    }

}
