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

    @Scheduled(
            cron = "${langora.auth.admin.session.cleanup.cron:0 30 3 * * ?}",
            zone = "${langora.auth.admin.session.cleanup.zone:Europe/Madrid}"
    )
    public void executeCleanup() {
        if (!sessionProperties.cleanup().enabled()) {
            log.info("[SessionCleanupTask] Admin session cleanup disabled");
            return;
        }

        Instant now = Instant.now();
        Instant cutoff = now.minus(sessionProperties.cleanup().retention());
        int batchSize = sessionProperties.cleanup().batchSize();

        log.info(
                "[SessionCleanupTask] Running cleanup. Purging sessions ended before {} (retention: {}, batchSize: {})",
                cutoff, sessionProperties.cleanup().retention(), batchSize
        );

        try {
            int deletedRows = sessionRepository.purgeSessionsEndedBefore(cutoff, batchSize);
            log.info("[SessionCleanupTask] Purged {} expired/revoked admin session(s).", deletedRows);
        } catch (Exception e) {
            log.error("[SessionCleanupTask] Failed to purge admin sessions: {}", e.getMessage(), e);
        }
    }
}
