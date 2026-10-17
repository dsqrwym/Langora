package org.dsqrwym.langora.infrastructure.web;

import org.springframework.http.HttpStatusCode;

public enum ErrorCode {
    /** Request validation failed. */
    VALIDATION_FAILED("Validation failed"),
    /** A required input value is missing. */
    VALIDATION_REQUIRED("A required value is missing"),
    /** An input value has an invalid format. */
    VALIDATION_INVALID_FORMAT("An input value has an invalid format"),
    /** An input value is outside its allowed length. */
    VALIDATION_LENGTH_OUT_OF_RANGE("An input value is outside its allowed length"),
    /** An input value violates a validation rule. */
    VALIDATION_INVALID_VALUE("An input value is invalid"),
    /** A combination of input values is invalid. */
    VALIDATION_INVALID_COMBINATION("The provided values cannot be used together"),

    /** Request syntax or body is malformed. */
    MALFORMED_REQUEST("Malformed request"),
    /** The HTTP method is not supported for this resource. */
    METHOD_NOT_ALLOWED("Method not allowed"),
    /** The requested response representation is unavailable. */
    NOT_ACCEPTABLE("Requested representation is not acceptable"),
    /** The request media type is unsupported. */
    UNSUPPORTED_MEDIA_TYPE("Unsupported media type"),
    /** The request could not be completed. */
    REQUEST_FAILED("Request failed"),

    /** Login credentials are invalid. */
    AUTH_INVALID_CREDENTIALS("Invalid username/email or password"),
    /** An access token is missing, expired, or invalid. */
    AUTH_ACCESS_TOKEN_INVALID("Authentication is required or the access token is invalid"),
    /** The refresh-token cookie is missing. */
    AUTH_REFRESH_TOKEN_MISSING("Refresh token is required"),
    /** A refresh token is invalid or expired. */
    AUTH_REFRESH_TOKEN_INVALID("Refresh token is invalid or expired"),
    /** The refresh-token session was revoked. */
    AUTH_SESSION_REVOKED("Session has been revoked"),
    /** The refresh-token session expired. */
    AUTH_SESSION_EXPIRED("Session has expired"),
    /** Refresh is already being completed by another request. */
    AUTH_REFRESH_IN_PROGRESS("Refresh token was already rotated by another request"),
    /** A rotated refresh token was replayed. */
    AUTH_REFRESH_REUSED("Refresh token reuse detected; all sessions were revoked"),
    /** The CSRF token is missing or invalid. */
    AUTH_CSRF_INVALID("Invalid or missing CSRF token"),
    /** The authenticated account is disabled. */
    AUTH_ACCOUNT_DISABLED("Account is not active"),

    /** The caller lacks permission for this operation. */
    ACCESS_DENIED("Access denied"),
    /** The requested resource does not exist. */
    RESOURCE_NOT_FOUND("Resource not found"),
    /** The resource state conflicts with this request. */
    RESOURCE_CONFLICT("Resource state conflicts with this request"),
    /** The caller exceeded an allowed request rate. */
    RATE_LIMITED("Too many requests"),
    /** An unexpected server failure occurred. */
    INTERNAL_ERROR("Internal server error");

    private final String defaultMessage;

    ErrorCode(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String defaultMessage() {
        return defaultMessage;
    }

    /**
     * Maps framework-level HTTP failures that have no more specific domain code.
     */
    public static ErrorCode fromHttpStatus(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> MALFORMED_REQUEST;
            case 401 -> AUTH_ACCESS_TOKEN_INVALID;
            case 403 -> ACCESS_DENIED;
            case 404 -> RESOURCE_NOT_FOUND;
            case 405 -> METHOD_NOT_ALLOWED;
            case 406 -> NOT_ACCEPTABLE;
            case 409 -> RESOURCE_CONFLICT;
            case 415 -> UNSUPPORTED_MEDIA_TYPE;
            case 422 -> VALIDATION_FAILED;
            case 429 -> RATE_LIMITED;
            default -> status.is5xxServerError() ? INTERNAL_ERROR : REQUEST_FAILED;
        };
    }
}
