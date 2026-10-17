package org.dsqrwym.langora.infrastructure.web;

public enum ErrorCode {

    NOT_FOUND,
    UNAUTHORIZED,
    ACCESS_DENIED,
    ACCOUNT_DISABLED,
    FORBIDDEN,
    CONFLICT,
    INTERNAL_ERROR,

    // Refresh token rotation — distinct codes so the frontend can act without parsing message strings
    /** Old token was just rotated by a concurrent request; wait for that request to finish. */
    AUTH_REFRESH_IN_PROGRESS,
    /** Old token was replayed outside the grace window; all sessions have been revoked. */
    AUTH_REFRESH_REUSED
}
