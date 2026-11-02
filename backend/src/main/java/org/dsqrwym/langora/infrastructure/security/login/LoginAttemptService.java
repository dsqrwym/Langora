package org.dsqrwym.langora.infrastructure.security.login;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dsqrwym.langora.infrastructure.security.login.store.LoginAttemptStore;
import org.dsqrwym.langora.infrastructure.util.TextUtils;
import org.dsqrwym.langora.infrastructure.web.exception.RateLimitedException;

/**
 * 先 CAPTCHA 后 IP 以及 账号锁
 */
@Slf4j
@RequiredArgsConstructor
public class LoginAttemptService {
    private static final String IP_KEY_PREFIX = "ip:";

    private final LoginAttemptStore store;
    private final LoginAttemptProperties properties;

    /**
     * 同时检查 IP 是否被封禁 是否需要 CAPTCHA
     *
     * @param ip IP 地址
     * @return 是否需要 CAPTCHA
     */
    public boolean checkIp(String ip) {
        final LoginAttemptStore.AttemptState state = store.inspect(ipKey(ip));

        if (state.reached(properties.ipBlockThreshold())) {
            log.debug("[LoginAttemptService] IP blocked({} failures, {}s left)", state.count(), state.retryAfter().toSeconds());
            throw new RateLimitedException(state.retryAfter());
        }

        return state.reached(properties.captchaThreshold());
    }

    public void recordIpFailure(String ip) {
        final LoginAttemptStore.AttemptState state = store.increment(ipKey(ip), properties.window());
        if (state.reached(properties.captchaThreshold())) {
            log.debug("[LoginAttemptService] IP {} reached the CAPTCHA threshold({} failures)", ip, state.count());
        }
    }

    public void checkAccount(LoginAttemptAccountKey<?> key) {
        final LoginAttemptStore.AttemptState state = store.inspect(key.value());

        if (state.reached(properties.accountBlockThreshold())) {
            log.debug("[LoginAttemptService] Account blocked({} failures, {}s left)", state.count(), state.retryAfter().toSeconds());
            throw new RateLimitedException(state.retryAfter());
        }
    }

    public void recordAccountFailure(LoginAttemptAccountKey<?> key) {
        final LoginAttemptStore.AttemptState state = store.increment(key.value(), properties.window());
        if (state.reached(properties.accountBlockThreshold())) {
            log.debug("[LoginAttemptService] Account {} reached the block threshold({} failures)", key.value(), state.count());
        }
    }

    public void clearAccount(LoginAttemptAccountKey<?> key) {
        store.reset(key.value());
    }

    /**
     * null/空白统一成 unknown，避免 "ip:null" 和 "ip:" 变成两个独立的计数器。
     */
    private static String ipKey(String ip) {
        return IP_KEY_PREFIX + (TextUtils.defaultIfBlank(ip, "unknown"));
    }
}
