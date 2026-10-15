package org.dsqrwym.langora.infrastructure.web.exception;

import org.dsqrwym.langora.infrastructure.web.ErrorCode;
import org.springframework.http.HttpStatus;

public final class InternalServerException extends BusinessException {

    public InternalServerException(String msg) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, msg);
    }

    public InternalServerException() {
        this("Internal server error");
    }
}