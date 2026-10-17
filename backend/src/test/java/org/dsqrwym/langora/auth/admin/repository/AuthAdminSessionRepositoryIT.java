package org.dsqrwym.langora.auth.admin.repository;

import org.dsqrwym.langora.infrastructure.jooq.enums.AdminSessionRevocationReason;
import org.dsqrwym.langora.infrastructure.jooq.enums.AdminUserRole;
import org.dsqrwym.langora.infrastructure.jooq.enums.AdminUserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jooq.autoconfigure.JooqAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.dsqrwym.langora.infrastructure.jooq.Tables.ADMIN_USERS;
import static org.dsqrwym.langora.infrastructure.jooq.Tables.ADMIN_USER_SESSIONS;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs against a disposable PostgreSQL container. It verifies PostgreSQL-specific upsert and CAS semantics,
 * not merely mocked Repository method calls.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("it")
@Execution(ExecutionMode.SAME_THREAD)
class AuthAdminSessionRepositoryIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private DSLContext db;
    @Autowired
    private AuthAdminRepository adminRepository;
    @Autowired
    private AuthAdminSessionRepository sessionRepository;

    @AfterEach
    void cleanDatabase() {
        db.deleteFrom(ADMIN_USER_SESSIONS).execute();
        db.deleteFrom(ADMIN_USERS).execute();
    }

    @Test
    void usernameIsCaseSensitiveWhileEmailUsesNormalizedLowercaseStorage() {
        createUser("AdminUser", "admin@example.test");

        assertTrue(adminRepository.findByUsername("AdminUser").isPresent());
        assertFalse(adminRepository.findByUsername("adminuser").isPresent());
        assertTrue(adminRepository.findByEmail("admin@example.test").isPresent());
    }

    @Test
    void upsertForSameDeviceKeepsOriginalDeviceNameButRefreshesSessionData() {
        long userId = createUser("AdminOne", "one@example.test");
        Instant firstActive = Instant.parse("2026-01-01T10:00:00Z");
        UUID firstSession = UUID.randomUUID();
        UUID replacementSession = UUID.randomUUID();

        sessionRepository.upsert(firstSession, userId, "device-1", "Windows . Chrome",
                "127.0.0.1", "first-agent", firstActive.plusSeconds(3600), firstActive, "hash-first");
        sessionRepository.upsert(replacementSession, userId, "device-1", "Changed name must be ignored",
                "127.0.0.2", "second-agent", firstActive.plusSeconds(7200), firstActive.plusSeconds(60), "hash-second");

        var stored = db.selectFrom(ADMIN_USER_SESSIONS)
                .where(ADMIN_USER_SESSIONS.ADMIN_USER_ID.eq(userId))
                .fetchOne();

        assertNotNull(stored);
        assertEquals(replacementSession, stored.getId());
        assertEquals("Windows . Chrome", stored.getDeviceName());
        assertEquals("127.0.0.2", stored.getLastIp());
        assertEquals("second-agent", stored.getUserAgent());
        assertEquals("hash-second", stored.getRefreshTokenHash());
        assertEquals(firstActive.plusSeconds(60), stored.getLastActiveAt());
        assertEquals(1, db.fetchCount(ADMIN_USER_SESSIONS));
    }

    @Test
    void rotateRefreshTokenIsCompareAndSetAndStoresPreviousHash() {
        long userId = createUser("AdminTwo", "two@example.test");
        UUID sessionId = UUID.randomUUID();
        Instant now = Instant.parse("2026-01-01T10:00:00Z");
        sessionRepository.upsert(sessionId, userId, "device-2", "Windows . Chrome",
                "127.0.0.1", "agent", now.plusSeconds(3600), now, "old-hash");

        int first = sessionRepository.rotateRefreshToken(
                sessionId, "old-hash", "new-hash", "127.0.0.2", now.plusSeconds(7200), now.plusSeconds(1));
        int replay = sessionRepository.rotateRefreshToken(
                sessionId, "old-hash", "another-hash", "127.0.0.3", now.plusSeconds(7200), now.plusSeconds(2));

        var stored = db.selectFrom(ADMIN_USER_SESSIONS).where(ADMIN_USER_SESSIONS.ID.eq(sessionId)).fetchOne();
        assertNotNull(stored);
        assertEquals(1, first);
        assertEquals(0, replay);
        assertEquals("new-hash", stored.getRefreshTokenHash());
        assertEquals("old-hash", stored.getPreviousRefreshTokenHash());
        assertEquals("127.0.0.2", stored.getLastIp());
    }

    @Test
    void rotationCanSucceedOnlyOnceWhenTwoRequestsRace() throws Exception {
        long userId = createUser("AdminThree", "three@example.test");
        UUID sessionId = UUID.randomUUID();
        Instant now = Instant.now();
        sessionRepository.upsert(sessionId, userId, "device-3", "Windows . Chrome",
                "127.0.0.1", "agent", now.plusSeconds(3600), now, "old-hash");

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<Integer>> results = new ArrayList<>();
            for (int index = 0; index < 2; index++) {
                String newHash = "new-hash-" + index;
                results.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return sessionRepository.rotateRefreshToken(
                            sessionId, "old-hash", newHash, "127.0.0.1", now.plusSeconds(7200), Instant.now());
                }));
            }
            ready.await();
            start.countDown();

            assertEquals(1, results.get(0).get() + results.get(1).get());
        }
    }

    @Test
    void revokeOldSessionsRetainsOnlyMostRecentlyActiveSessions() {
        long userId = createUser("AdminFour", "four@example.test");
        Instant now = Instant.parse("2026-01-01T10:00:00Z");
        UUID oldest = UUID.randomUUID();
        UUID middle = UUID.randomUUID();
        UUID newest = UUID.randomUUID();
        sessionRepository.upsert(oldest, userId, "device-old", "Windows . Chrome", "127.0.0.1", "agent",
                now.plusSeconds(3600), now.minusSeconds(30), "hash-old");
        sessionRepository.upsert(middle, userId, "device-middle", "Windows . Chrome", "127.0.0.1", "agent",
                now.plusSeconds(3600), now.minusSeconds(20), "hash-middle");
        sessionRepository.upsert(newest, userId, "device-new", "Windows . Chrome", "127.0.0.1", "agent",
                now.plusSeconds(3600), now.minusSeconds(10), "hash-new");

        sessionRepository.revokeOldsSessions(userId, 2, now);

        var oldestRecord = db.selectFrom(ADMIN_USER_SESSIONS).where(ADMIN_USER_SESSIONS.ID.eq(oldest)).fetchOne();
        var middleRecord = db.selectFrom(ADMIN_USER_SESSIONS).where(ADMIN_USER_SESSIONS.ID.eq(middle)).fetchOne();
        var newestRecord = db.selectFrom(ADMIN_USER_SESSIONS).where(ADMIN_USER_SESSIONS.ID.eq(newest)).fetchOne();
        assertNotNull(oldestRecord);
        assertNotNull(middleRecord);
        assertNotNull(newestRecord);
        assertEquals(AdminSessionRevocationReason.SESSION_LIMIT_EXCEEDED, oldestRecord.getRevokedReason());
        assertNotNull(oldestRecord.getRevokedAt());
        assertNull(middleRecord.getRevokedAt());
        assertNull(newestRecord.getRevokedAt());
    }

    private long createUser(String username, String email) {
        var record = db.insertInto(ADMIN_USERS)
                .set(ADMIN_USERS.USERNAME, username)
                .set(ADMIN_USERS.EMAIL, email)
                .set(ADMIN_USERS.PASSWORD_HASH, "not-used-by-repository-test")
                .set(ADMIN_USERS.ROLE, AdminUserRole.ADMIN)
                .set(ADMIN_USERS.STATUS, AdminUserStatus.ACTIVE)
                .returning(ADMIN_USERS.ID)
                .fetchOne();
        assertNotNull(record);
        return record.getId();
    }
}
