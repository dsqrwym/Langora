package org.dsqrwym.langora.infrastructure.security.login;

import java.util.Locale;

/**
 * Namespaced key for an account counter.
 *
 * <p>IP counters are deliberately shared across all account types, but account counters must not
 * collide: an admin with id 7 and a teacher with id 7 are different subjects. The type is an enum,
 * so callers use their domain enum ({@code UserTypes.ADMIN}) instead of a free-form string, while
 * this infrastructure package stays independent of the auth package.
 *
 * <p>Current key shape: {@code account:<type>:<id>}, for example {@code account:admin:7}.
 */
public record LoginAttemptAccountKey<T extends Enum<T>>(T type, String id) {

    public LoginAttemptAccountKey {
        if (type == null || id == null || id.isBlank()) {
            throw new IllegalArgumentException("type and id must not be blank");
        }
        id = id.trim();
        if (id.indexOf(':') >= 0) {
            throw new IllegalArgumentException("id must not contain ':'");
        }
    }

    public static <T extends Enum<T>> LoginAttemptAccountKey<T> of(T type, String id) {
        return new LoginAttemptAccountKey<>(type, id);
    }

    public String value() {
        return "account:" + type.name().toLowerCase(Locale.ROOT) + ":" + id;
    }
}
