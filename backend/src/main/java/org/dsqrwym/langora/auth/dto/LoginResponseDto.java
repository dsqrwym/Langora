package org.dsqrwym.langora.auth.dto;

import org.dsqrwym.langora.auth.UserTypes;

import java.util.List;
import java.util.UUID;

@lombok.Data
public class LoginResponseDto {
    private String accessToken;
    private String refreshToken;
    private String csrfToken;
    private Long userId;
    private String username;
    private String email;
    private UserTypes userTypes;
    private List<String> roles;
    private UUID sessionId;

    public LoginResponseDto(String accessToken, String refreshToken, Long userId, String username, String email, UserTypes userTypes, List<String> roles, UUID sessionId) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.userTypes = userTypes;
        this.roles = roles;
        this.sessionId = sessionId;
    }
}
