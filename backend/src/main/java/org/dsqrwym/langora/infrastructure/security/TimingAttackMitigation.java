package org.dsqrwym.langora.infrastructure.security;

import lombok.Getter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.UUID;

/**
 * Holds a pre-computed bcrypt hash used to equalize the cost of authentication
 * attempts for existing and non-existing users, preventing username enumeration
 * via response-time differences.
 * Generated once at startup from a random, never-reused plaintext. The cost
 * factor matches real password hashes so will take the same time.
 */
@Component
public class TimingAttackMitigation {
    /**
     * Pre-computed dummy password hash used to equalize the cost of
     * authentication attempts for existing and non-existing users.
     */
    @Getter
    public final String dummyPasswordHash;

    public TimingAttackMitigation(PasswordEncoder passwordEncoder) {
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }
}
