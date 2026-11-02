package org.dsqrwym.langora.infrastructure.security.captcha;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TurnstileResponse(
        boolean success,
        String hostname,
        String action,
        @JsonProperty("error-codes") List<String> errorCodes
) {
}
