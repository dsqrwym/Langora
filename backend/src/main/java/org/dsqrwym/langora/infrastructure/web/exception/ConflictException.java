package org.dsqrwym.langora.infrastructure.web.exception;

import org.dsqrwym.langora.infrastructure.web.ErrorCode;
import org.springframework.http.HttpStatus;

public final class ConflictException extends BusinessException {
    public ConflictException(ErrorCode code, String msg) {
        super(HttpStatus.CONFLICT, code, msg);
    }
    public ConflictException(String msg) {
        super(HttpStatus.CONFLICT, ErrorCode.CONFLICT, msg);
    }
}
