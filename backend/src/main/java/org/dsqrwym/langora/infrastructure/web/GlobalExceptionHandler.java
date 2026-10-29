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

import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * Cath all exceptions for Spring MVC built-ins.
     * <p>
     * 统一处理 Spring MVC 内置异常，转换成 ApiResponse 错误契约。
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
        HttpStatus responseStatus = resolvedStatus != null ? resolvedStatus : HttpStatus.INTERNAL_SERVER_ERROR;

        return ResponseEntity.status(responseStatus).headers(headers).body(
                ApiResponse.error(responseStatus, ErrorCode.fromHttpStatus(responseStatus), null)
        );
    }

    /**
     * Bean Validation on @RequestBody
     * data: {"field": "field_name", "code": "VALIDATION_*"}
     * <p>
     * 处理 @RequestBody 的 Bean Validation 失败。
     *
     * @param ex      the exception to handle
     * @param headers the headers to be written to the response
     * @param status  the selected response status
     * @param request the current request
     * @return the response entity
     */
    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, @NonNull HttpHeaders headers, @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        List<ValidationErrorDetail> details = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> details.add(
                new ValidationErrorDetail(error.getField(), validationCode(error.getCode()), error.getDefaultMessage())
        ));
        ex.getBindingResult().getGlobalErrors().forEach(error -> details.add(
                new ValidationErrorDetail("$request", validationCode(error.getCode()), error.getDefaultMessage())
        ));

        return ResponseEntity.status(status).headers(headers).body(
                ApiResponse.error(status, ErrorCode.VALIDATION_FAILED, details)
        );
    }

    /**
     * RequestParam, PathVariable validation
     * <p>
     * 处理 @RequestParam / @PathVariable 等方法参数校验失败。
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
                        error -> new ParamErrorDetail(
                                String.valueOf(error.getMethodParameter().getParameterName()),
                                ErrorCode.VALIDATION_INVALID_VALUE
                        )
                )
                .toList();
        return ResponseEntity.badRequest().body(
                ApiResponse.error(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, details)
        );
    }

    /**
     * Method level validation @Validated
     * <p>
     * 处理 @Validated 方法级校验失败。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> onConstraintViolation(ConstraintViolationException ex) {
        List<ValidationErrorDetail> details = ex.getConstraintViolations().stream()
                .map(error -> new ValidationErrorDetail(
                        error.getPropertyPath().toString(),
                        validationCode(error.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName()),
                        error.getMessage()
                )).toList();

        return ResponseEntity.badRequest().body(
                ApiResponse.error(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, details)
        );
    }

    /**
     * Business exception
     *
     * 处理业务异常（4xx），直接使用异常携带的 status/code。
     *
     * @param ex the exception to handle
     * @return the response entity
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> onBusinessException(BusinessException ex) {
        return ResponseEntity.status(ex.status).body(
                ApiResponse.error(ex.status, ex.code, null)
        );
    }

    /**
     * Access denied throw from controllers
     * <p>
     * 处理控制器抛出的权限不足异常。
     *
     * @param ex the exception to handle
     * @return the response entity
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> onAccessDeniedException(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ApiResponse.error(HttpStatus.FORBIDDEN, ErrorCode.ACCESS_DENIED, null)
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> onUnknownException(Exception ex) {
        logger.error("Unknown exception", ex);
        return ResponseEntity.internalServerError().body(
                ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, null)
        );
    }

    private static ErrorCode validationCode(String constraintName) {
        if (constraintName == null) {
            return ErrorCode.VALIDATION_INVALID_VALUE;
        }

        return switch (constraintName) {
            case "NotBlank", "NotEmpty", "NotNull" -> ErrorCode.VALIDATION_REQUIRED;
            case "Size", "Length" -> ErrorCode.VALIDATION_LENGTH_OUT_OF_RANGE;
            case "AssertTrue", "AssertFalse" -> ErrorCode.VALIDATION_INVALID_COMBINATION;
            case "Email", "Pattern", "UUID", "AdminUsername", "StrongPassword" -> ErrorCode.VALIDATION_INVALID_FORMAT;
            default -> ErrorCode.VALIDATION_INVALID_VALUE;
        };
    }

}
