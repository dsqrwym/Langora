package org.dsqrwym.langora.infrastructure.web.exception;

import org.dsqrwym.langora.infrastructure.web.ErrorCode;
import org.springframework.http.HttpStatus;

public final class ForbiddenException extends BusinessException {
    public ForbiddenException(ErrorCode code) {
        super(HttpStatus.FORBIDDEN, code);
    }

    public ForbiddenException() {
        this(ErrorCode.ACCESS_DENIED);
    }

    /**
     *
     * @return Account Disabled ForbiddenException
     */
    public static ForbiddenException accountDisabled() {
        return new ForbiddenException(ErrorCode.AUTH_ACCOUNT_DISABLED);
    }
}
