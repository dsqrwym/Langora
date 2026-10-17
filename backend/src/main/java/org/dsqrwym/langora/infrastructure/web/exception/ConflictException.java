package org.dsqrwym.langora.infrastructure.web.exception;

import org.dsqrwym.langora.infrastructure.web.ErrorCode;
import org.springframework.http.HttpStatus;

public final class ConflictException extends BusinessException {
    public ConflictException(ErrorCode code) {
        super(HttpStatus.CONFLICT, code);
    }

    public ConflictException() {
        this(ErrorCode.RESOURCE_CONFLICT);
    }
}
