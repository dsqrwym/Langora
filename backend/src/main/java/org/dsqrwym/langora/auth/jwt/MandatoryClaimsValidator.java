package org.dsqrwym.langora.auth.jwt;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

public class MandatoryClaimsValidator implements OAuth2TokenValidator<Jwt> {
    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        if (token.getClaim(UserJwtPayload.CLAIM_NAME_USER_ID) == null
                || token.getClaimAsString(UserJwtPayload.CLAIM_NAME_USERNAME) == null
                || token.getClaimAsString(UserJwtPayload.CLAIM_NAME_USER_TYPE) == null
                || token.getClaimAsString(UserJwtPayload.CLAIM_NAME_SESSION_ID) == null) {
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Missing required claims", null));
        }
        List<String> roles = token.getClaimAsStringList(UserJwtPayload.CLAIM_NAME_ROLES);
        if (roles == null || roles.isEmpty()) {
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Roles claim must not be empty", null));
        }
        return OAuth2TokenValidatorResult.success();
    }
}
