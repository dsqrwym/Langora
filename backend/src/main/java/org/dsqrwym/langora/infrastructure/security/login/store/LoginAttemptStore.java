package org.dsqrwym.langora.infrastructure.security.login.store;

import java.time.Duration;

/**
 * Counter storage for login attempts
 */
public interface LoginAttemptStore {
    record AttemptState(long count, Duration retryAfter) {
        public static final AttemptState EMPTY = new AttemptState(0L, Duration.ZERO);
        public boolean reached(int threshold) {
            return count >= threshold;
        }
    }

    /**
     * Only read
     * @param key The key to inspect
     * @return AttemptState
     */
    AttemptState inspect(String key);

    /**
     * Atomic increment the counter for the given key within the specified window
     * @param key The key to increment
     * @param window The time window to consider for the increment
     * @return AttemptState
     */
    AttemptState increment(String key, Duration window);

    void reset(String key);
}
