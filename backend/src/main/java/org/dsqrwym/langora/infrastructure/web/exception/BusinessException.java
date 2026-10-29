package org.dsqrwym.langora.infrastructure.web.exception;

import org.dsqrwym.langora.infrastructure.web.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * Business exception.
 * To disable stack trace because business exceptions (4xxm for users)
 * are on the hot path and should not be logged.
 * <p>
 * 业务异常基类。
 * 关闭堆栈是为了让 4xx 这类热路径异常不产生无意义的堆栈日志。
 * 使用 sealed 限定子类，避免任意 RuntimeException 被当作业务异常处理。
 */
public sealed class BusinessException extends RuntimeException permits ConflictException, ForbiddenException, InternalServerException, NotFoundException, RateLimitedException, UnauthorizedException {
    public final HttpStatus status;
    public final ErrorCode code;

    public BusinessException(HttpStatus status, ErrorCode code) {
        super(code.defaultMessage(), null, true, false);
        this.code = code;
        this.status = status;
    }
}
