package org.dsqrwym.langora.infrastructure.web.exception;

import org.dsqrwym.langora.infrastructure.web.ErrorCode;
import org.springframework.http.HttpStatus;

public final class NotFoundException extends BusinessException {
    public NotFoundException(String msg) {
        super(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, msg);
    }

    public NotFoundException() {
        this("Not found");
    }

}
