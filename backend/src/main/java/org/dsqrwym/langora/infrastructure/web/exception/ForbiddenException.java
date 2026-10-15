package org.dsqrwym.langora.infrastructure.web.exception;

import org.dsqrwym.langora.infrastructure.web.ErrorCode;
import org.springframework.http.HttpStatus;

public final class ForbiddenException extends BusinessException {
    public ForbiddenException(ErrorCode code, String msg) {
        super(HttpStatus.FORBIDDEN, code, msg);
    }

    public ForbiddenException(String msg) {
        super(HttpStatus.FORBIDDEN, ErrorCode.ACCESS_DENIED, msg);
    }

    public ForbiddenException() {
        this("Access denied");
    }

    /**
     *
     * @return Account Disabled ForbiddenException
     */
    public static ForbiddenException accountDisabled() {
        return new ForbiddenException(ErrorCode.ACCOUNT_DISABLED, "Account is not active");
    }
}