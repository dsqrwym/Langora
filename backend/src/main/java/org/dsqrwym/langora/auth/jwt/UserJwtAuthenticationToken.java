package org.dsqrwym.langora.auth.jwt;

import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;

/**
 * Custom Authentication token representing an authenticated JWT user session.
 */
public class UserJwtAuthenticationToken extends AbstractAuthenticationToken {
    private final UserJwtPayload principal;
    @Getter
    private final Jwt token;

    public UserJwtAuthenticationToken(UserJwtPayload principal, Jwt token, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.token = token;
        // Without this, Spring Security treats the user as unauthenticated and rejects requests with 401
        setAuthenticated(true);
    }

    @Override
    public @Nullable Jwt getCredentials() {
        return token;
    }

    // principal is always set in the constructor — never null despite parent's @Nullable contract
    @Override
    public @NonNull UserJwtPayload getPrincipal() {
        return principal;
    }

}
