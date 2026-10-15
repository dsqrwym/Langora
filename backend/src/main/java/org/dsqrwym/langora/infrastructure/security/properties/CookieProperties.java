package org.dsqrwym.langora.infrastructure.security.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "langora.security.cookie")
public record CookieProperties(
        String domain,
        boolean secure,
        String sameSite,
        String refreshTokenPath
) {
    public CookieProperties {
        if (refreshTokenPath == null) {
            refreshTokenPath = "/auth/admin/refresh";
        }
        if (sameSite == null) {
            sameSite = "Lax";
        }
    }
}
