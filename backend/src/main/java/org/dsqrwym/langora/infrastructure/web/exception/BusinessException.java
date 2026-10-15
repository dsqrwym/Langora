package org.dsqrwym.langora.infrastructure.web.exception;

import org.dsqrwym.langora.infrastructure.web.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * Business exception.
 * To disable stack trace because business exceptions (4xxm for users)
 * are on the hot path and should not be logged.
 */
public sealed class BusinessException extends RuntimeException permits ConflictException, ForbiddenException, InternalServerException, NotFoundException, UnauthorizedException {
    public final HttpStatus status;
    public final ErrorCode code;
    public final String msg;

    public BusinessException(HttpStatus status, ErrorCode code, String msg) {
        super(msg, null, true, false);
        this.code = code;
        this.status = status;
        this.msg = msg;
    }
}
