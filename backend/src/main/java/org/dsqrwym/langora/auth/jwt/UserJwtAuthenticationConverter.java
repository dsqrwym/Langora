package org.dsqrwym.langora.auth.jwt;

import org.dsqrwym.langora.auth.UserTypes;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class UserJwtAuthenticationConverter implements Converter<Jwt, UserJwtAuthenticationToken> {

    @Override
    public UserJwtAuthenticationToken convert(Jwt source) {
        // userId: JWT spec allows small integers to deserialise as Integer, not Long
        Number rawUserId = source.getClaim(UserJwtPayload.CLAIM_NAME_USER_ID);
        long userId;
        if (rawUserId == null) {
            userId = Long.parseLong(requireClaim(source.getSubject(), "sub / userId"));
        } else {
            userId = rawUserId.longValue();
        }

        String rawUserType = requireClaim(
                source.getClaimAsString(UserJwtPayload.CLAIM_NAME_USER_TYPE), "userTypes");
        UserTypes userType;
        try {
            userType = UserTypes.valueOf(rawUserType);
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException("Invalid token: unknown userTypes value: " + rawUserType);
        }

        String username = requireClaim(
                source.getClaimAsString(UserJwtPayload.CLAIM_NAME_USERNAME), "username");

        String email = requireClaim(
                source.getClaimAsString(UserJwtPayload.CLAIM_NAME_EMAIL), "email");

        List<String> roles = source.getClaimAsStringList(UserJwtPayload.CLAIM_NAME_ROLES);
        if (roles == null || roles.isEmpty()) {
            throw new BadCredentialsException("Invalid token: roles claim is missing or empty");
        }

        String rawSessionId = requireClaim(
                source.getClaimAsString(UserJwtPayload.CLAIM_NAME_SESSION_ID), "sessionId");
        UUID sessionId;
        try {
            sessionId = UUID.fromString(rawSessionId);
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException("Invalid token: sessionId is not a valid UUID");
        }

        UserJwtPayload payload = new UserJwtPayload(userId, username, email, userType, roles, sessionId);
        return new UserJwtAuthenticationToken(payload, source, payload.getAuthorities());
    }

    /** Throw {@link BadCredentialsException} if {@code value} is null or blank. */
    private static String requireClaim(String value, String claimName) {
        if (value == null || value.isBlank()) {
            throw new BadCredentialsException("Invalid token: missing claim '" + claimName + "'");
        }
        return value;
    }
}
