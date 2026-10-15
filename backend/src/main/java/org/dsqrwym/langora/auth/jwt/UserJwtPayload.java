package org.dsqrwym.langora.auth.jwt;

import org.dsqrwym.langora.auth.UserTypes;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Representation of JWT Claims
 */
public record UserJwtPayload(
        Long userId,
        String username,
        String email,
        UserTypes userTypes,
        List<String> roles,
        UUID sessionId
) implements UserDetails {
    public static String CLAIM_NAME_USER_ID = "userId";
    public static String CLAIM_NAME_USERNAME = "username";
    public static String CLAIM_NAME_EMAIL = "email";
    public static String CLAIM_NAME_USER_TYPE = "userTypes";
    public static String CLAIM_NAME_ROLES = "roles";
    public static String CLAIM_NAME_SESSION_ID = "sessionId";

    public static UserJwtPayload forAdmin(Long id, String username, String email, String adminRole, UUID sessionId) {
        return new UserJwtPayload(id, username, email, UserTypes.ADMIN, List.of(adminRole), sessionId);
    }

    @Override
    @NonNull
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (roles.isEmpty()) {
            // Throw exception Spring Security will return HTTP 401 (Unauthorized/Token invalid)
            throw new BadCredentialsException("Invalid token: roles claim is missing");
        }
        List<GrantedAuthority> authorities = new ArrayList<>(roles.size() + 1);
        authorities.add(new SimpleGrantedAuthority("TYPE_" + userTypes.name()));
        for (String role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        return authorities;
    }

    /**
     * JWT always returns null for password
     *
     * @return null
     */
    @Override
    public @Nullable String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return username;
    }

}
