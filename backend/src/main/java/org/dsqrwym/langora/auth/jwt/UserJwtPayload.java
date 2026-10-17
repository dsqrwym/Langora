package org.dsqrwym.langora.auth.jwt;

import org.dsqrwym.langora.auth.UserTypes;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Typed representation of claims stored inside a signed access token.
 * All fields are guaranteed non-null — the converter rejects any token with missing claims.
 */
public record UserJwtPayload(
        Long userId,
        String username,
        String email,
        UserTypes userTypes,
        List<String> roles,
        UUID sessionId
) implements UserDetails {

    public static final String CLAIM_NAME_USER_ID = "userId";
    public static final String CLAIM_NAME_USERNAME = "username";
    public static final String CLAIM_NAME_EMAIL = "email";
    public static final String CLAIM_NAME_USER_TYPE = "userTypes";
    public static final String CLAIM_NAME_ROLES = "roles";
    public static final String CLAIM_NAME_SESSION_ID = "sessionId";

    public static UserJwtPayload forAdmin(Long id, String username, String email, String adminRole, UUID sessionId) {
        return new UserJwtPayload(id, username, email, UserTypes.ADMIN, List.of(adminRole), sessionId);
    }

    @Override
    @NonNull
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // roles non-null and non-empty is enforced by UserJwtAuthenticationConverter before this is called
        List<GrantedAuthority> authorities = new ArrayList<>(roles.size() + 1);
        authorities.add(new SimpleGrantedAuthority("TYPE_" + userTypes.name()));
        for (String role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        return authorities;
    }

    /**
     * JWT-based auth has no password. Spring Security UserDetails contract allows null here.
     */
    @Override
    public @Nullable String getPassword() {
        return null;
    }

    @Override
    @NonNull
    public String getUsername() {
        return username;
    }
}
