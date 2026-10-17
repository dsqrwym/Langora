package org.dsqrwym.langora.infrastructure.web.exception;

import org.dsqrwym.langora.infrastructure.web.ErrorCode;
import org.springframework.http.HttpStatus;

public final class UnauthorizedException extends BusinessException {
    public UnauthorizedException(String msg) {
        super(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, msg);
    }

    public UnauthorizedException(ErrorCode code, String msg) {
        super(HttpStatus.UNAUTHORIZED, code, msg);
    }

    public UnauthorizedException() {
        this("Unauthorized or invalid credentials");
    }
}
