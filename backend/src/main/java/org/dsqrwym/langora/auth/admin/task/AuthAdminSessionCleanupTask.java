package org.dsqrwym.langora.auth.admin.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dsqrwym.langora.auth.admin.config.AuthAdminSessionProperties;
import org.dsqrwym.langora.auth.admin.repository.AuthAdminSessionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthAdminSessionCleanupTask {
    private final AuthAdminSessionRepository sessionRepository;
    private final AuthAdminSessionProperties sessionProperties;

    @Scheduled(cron = "${langora.auth.admin.session.cleanup.cron:0 30 3 * * ?}")
    public void executeCleanup() {
        if (!sessionProperties.cleanup().enabled()) {
            log.info("[SessionCleanupTask] Admin session cleanup disabled");
            return;
        }

        Instant now = Instant.now();
        Instant cutoff = now.minus(sessionProperties.cleanup().retention());

        log.info(
                "[SessionCleanupTask] Running cleanup. Purging sessions ended before {} (retention: {})",
                cutoff, sessionProperties.cleanup().retention()
        );

        try {
            int deletedRows = sessionRepository.purgeSessionsEndedBefore(cutoff);
            log.info("[SessionCleanupTask] Purged {} expired/revoked admin session(s).", deletedRows);
        } catch (Exception e) {
            log.error("[SessionCleanupTask] Failed to purge admin sessions: {}", e.getMessage(), e);
        }
    }
}
