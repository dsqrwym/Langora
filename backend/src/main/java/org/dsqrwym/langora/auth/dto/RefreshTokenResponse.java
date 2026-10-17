package org.dsqrwym.langora.auth.dto;

import lombok.Data;

@Data
public class RefreshTokenResponse {
    public String accessToken;
    public String refreshToken;
    /** HMAC(secret, sessionId) — stable for the session lifetime. Store in localStorage for page-reload recovery. */
    public String csrfToken;

    public RefreshTokenResponse(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
