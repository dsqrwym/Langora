package org.dsqrwym.langora.infrastructure.web.exception;

import org.dsqrwym.langora.infrastructure.web.ErrorCode;
import org.springframework.http.HttpStatus;

public final class NotFoundException extends BusinessException {
    public NotFoundException() {
        super(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND);
    }

}
