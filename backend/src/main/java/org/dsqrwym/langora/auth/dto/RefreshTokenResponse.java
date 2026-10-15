package org.dsqrwym.langora.auth.dto;

import lombok.Data;

@Data
public class RefreshTokenResponse {
    public String accessToken;
    public String refreshToken;

    public RefreshTokenResponse(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
