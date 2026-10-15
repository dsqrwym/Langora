package org.dsqrwym.langora.auth.jwt;

import org.dsqrwym.langora.auth.UserTypes;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

@Component
public class UserJwtAuthenticationConverter implements Converter<Jwt, UserJwtAuthenticationToken> {
    @Override
    public UserJwtAuthenticationToken convert(Jwt source) {
        // Prevents ClassCastException when the JSON parser deserializes small IDs as Integer instead of Long
        Number rawUserId = source.getClaim(UserJwtPayload.CLAIM_NAME_USER_ID);
        long userId;
        if (rawUserId == null) {
            userId = Long.parseLong(Objects.requireNonNull(source.getSubject()));
        }else {
            userId = rawUserId.longValue();
        }

        UserTypes userType = UserTypes.valueOf(source.getClaimAsString(UserJwtPayload.CLAIM_NAME_USER_TYPE));

        final UserJwtPayload payload = new UserJwtPayload(
                userId,
                source.getClaimAsString(UserJwtPayload.CLAIM_NAME_USERNAME),
                source.getClaimAsString(UserJwtPayload.CLAIM_NAME_EMAIL),
                userType,
                source.getClaimAsStringList(UserJwtPayload.CLAIM_NAME_ROLES),
                UUID.fromString(Objects.requireNonNull(source.getClaimAsString(UserJwtPayload.CLAIM_NAME_SESSION_ID)))
        );

        return new UserJwtAuthenticationToken(payload, source, payload.getAuthorities());
    }
}
