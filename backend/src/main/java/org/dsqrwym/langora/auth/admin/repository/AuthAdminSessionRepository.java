package org.dsqrwym.langora.auth.admin.repository;

import lombok.RequiredArgsConstructor;
import org.dsqrwym.langora.infrastructure.jooq.enums.AdminSessionRevocationReason;
import org.dsqrwym.langora.infrastructure.jooq.enums.AdminUserRole;
import org.dsqrwym.langora.infrastructure.jooq.enums.AdminUserStatus;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.dsqrwym.langora.infrastructure.jooq.Tables.ADMIN_USERS;
import static org.dsqrwym.langora.infrastructure.jooq.Tables.ADMIN_USER_SESSIONS;
import static org.jooq.Records.mapping;
import static org.jooq.impl.DSL.select;

@Repository
@RequiredArgsConstructor
public class AuthAdminSessionRepository {
    private final DSLContext db;

    public void lockAdminUserForSessionUpdate(Long userId) {
        db.select(ADMIN_USERS.ID)
                .from(ADMIN_USERS)
                .where(ADMIN_USERS.ID.eq(userId))
                .forUpdate()
                .fetchOne();
    }

    public void revokeOldsSessions(Long userId, Integer maxSessions, Instant now) {
        db.update(ADMIN_USER_SESSIONS)
                .set(ADMIN_USER_SESSIONS.REVOKED_AT, now)
                .set(ADMIN_USER_SESSIONS.REVOKED_REASON, AdminSessionRevocationReason.SESSION_LIMIT_EXCEEDED)
                .where(ADMIN_USER_SESSIONS.ADMIN_USER_ID.eq(userId))
                .and(ADMIN_USER_SESSIONS.REVOKED_AT.isNull())
                .and(ADMIN_USER_SESSIONS.ID.notIn(
                        select(ADMIN_USER_SESSIONS.ID)
                                .from(ADMIN_USER_SESSIONS)
                                .where(ADMIN_USER_SESSIONS.ADMIN_USER_ID.eq(userId))
                                .and(ADMIN_USER_SESSIONS.REVOKED_AT.isNull())
                                .orderBy(ADMIN_USER_SESSIONS.LAST_ACTIVE_AT.desc())
                                .limit(maxSessions)
                ))
                .execute();
    }

    public void upsert(UUID sessionId, Long userId, String deviceUuid, String deviceName, String lastIp, String userAgent, Instant expiresAt, Instant lastActiveAt, String refreshTokenHash) {
        db.insertInto(ADMIN_USER_SESSIONS)
                .set(ADMIN_USER_SESSIONS.ID, sessionId)
                .set(ADMIN_USER_SESSIONS.ADMIN_USER_ID, userId)
                .set(ADMIN_USER_SESSIONS.DEVICE_ID, deviceUuid)
                .set(ADMIN_USER_SESSIONS.DEVICE_NAME, deviceName)
                .set(ADMIN_USER_SESSIONS.LAST_IP, lastIp)
                .set(ADMIN_USER_SESSIONS.USER_AGENT, userAgent)
                .set(ADMIN_USER_SESSIONS.EXPIRES_AT, expiresAt)
                .set(ADMIN_USER_SESSIONS.LAST_ACTIVE_AT, lastActiveAt)
                .set(ADMIN_USER_SESSIONS.REFRESH_TOKEN_HASH, refreshTokenHash)
                .set(ADMIN_USER_SESSIONS.CREATED_AT, lastActiveAt)
                .onConflict(ADMIN_USER_SESSIONS.ADMIN_USER_ID, ADMIN_USER_SESSIONS.DEVICE_ID)
                .doUpdate()
                .set(ADMIN_USER_SESSIONS.ID, sessionId)
                .set(ADMIN_USER_SESSIONS.LAST_IP, lastIp)
                .set(ADMIN_USER_SESSIONS.USER_AGENT, userAgent)
                .set(ADMIN_USER_SESSIONS.EXPIRES_AT, expiresAt)
                .set(ADMIN_USER_SESSIONS.LAST_ACTIVE_AT, lastActiveAt)
                .set(ADMIN_USER_SESSIONS.REFRESH_TOKEN_HASH, refreshTokenHash)
                .setNull(ADMIN_USER_SESSIONS.PREVIOUS_REFRESH_TOKEN_HASH)
                .setNull(ADMIN_USER_SESSIONS.REVOKED_AT)
                .setNull(ADMIN_USER_SESSIONS.REVOKED_REASON)
                .execute();
    }

    public record SelectedSessionWithUser(
            UUID sessionId,
            Long adminUserId,
            String refreshTokenHash,
            String previousRefreshTokenHash,
            Instant revokedAt,
            Instant expiresAt,
            Instant lastActiveAt,
            String deviceName,
            String lastIp,
            String userAgent,
            String username,
            String email,
            AdminUserRole role,
            AdminUserStatus status
    ) {
    }

    public Optional<SelectedSessionWithUser> getSessionByCurrentOrPreviousRefreshTokenHash(String refreshTokenHash) {
        return db.select(
                        ADMIN_USER_SESSIONS.ID,
                        ADMIN_USER_SESSIONS.ADMIN_USER_ID,
                        ADMIN_USER_SESSIONS.REFRESH_TOKEN_HASH,
                        ADMIN_USER_SESSIONS.PREVIOUS_REFRESH_TOKEN_HASH,
                        ADMIN_USER_SESSIONS.REVOKED_AT,
                        ADMIN_USER_SESSIONS.EXPIRES_AT,
                        ADMIN_USER_SESSIONS.LAST_ACTIVE_AT,
                        ADMIN_USER_SESSIONS.DEVICE_NAME,
                        ADMIN_USER_SESSIONS.LAST_IP,
                        ADMIN_USER_SESSIONS.USER_AGENT,
                        ADMIN_USERS.USERNAME,
                        ADMIN_USERS.EMAIL,
                        ADMIN_USERS.ROLE,
                        ADMIN_USERS.STATUS
                )
                .from(ADMIN_USER_SESSIONS)
                .join(ADMIN_USERS).on(ADMIN_USER_SESSIONS.ADMIN_USER_ID.eq(ADMIN_USERS.ID))
                .where(ADMIN_USER_SESSIONS.REFRESH_TOKEN_HASH.eq(refreshTokenHash))
                .or(ADMIN_USER_SESSIONS.PREVIOUS_REFRESH_TOKEN_HASH.eq(refreshTokenHash))
                .fetchOptional(mapping(SelectedSessionWithUser::new));
    }

    public int revokeAllByUser(Long userId, AdminSessionRevocationReason reason, Instant revokedAt) {
        return db.update(ADMIN_USER_SESSIONS)
                .set(ADMIN_USER_SESSIONS.REVOKED_AT, revokedAt)
                .set(ADMIN_USER_SESSIONS.REVOKED_REASON, reason)
                .where(ADMIN_USER_SESSIONS.ADMIN_USER_ID.eq(userId))
                .and(ADMIN_USER_SESSIONS.REVOKED_AT.isNull())
                .execute();
    }

    /**
     * Revokes a single session by ID. Used for logout.
     */
    public void revokeSession(UUID sessionId, AdminSessionRevocationReason reason, Instant revokedAt) {
        db.update(ADMIN_USER_SESSIONS)
                .set(ADMIN_USER_SESSIONS.REVOKED_AT, revokedAt)
                .set(ADMIN_USER_SESSIONS.REVOKED_REASON, reason)
                .where(ADMIN_USER_SESSIONS.ID.eq(sessionId))
                .and(ADMIN_USER_SESSIONS.REVOKED_AT.isNull())
                .execute();
    }

    public int rotateRefreshToken(UUID sessionId, String expectedCurrentHash, String newHash, String lastIp, Instant expiresAt, Instant now) {
        return db.update(ADMIN_USER_SESSIONS)
                .set(ADMIN_USER_SESSIONS.PREVIOUS_REFRESH_TOKEN_HASH, expectedCurrentHash)
                .set(ADMIN_USER_SESSIONS.REFRESH_TOKEN_HASH, newHash)
                .set(ADMIN_USER_SESSIONS.EXPIRES_AT, expiresAt)
                .set(ADMIN_USER_SESSIONS.LAST_ACTIVE_AT, now)
                .set(ADMIN_USER_SESSIONS.LAST_IP, lastIp)
                .where(ADMIN_USER_SESSIONS.ID.eq(sessionId))
                .and(ADMIN_USER_SESSIONS.REFRESH_TOKEN_HASH.eq(expectedCurrentHash))
                .and(ADMIN_USER_SESSIONS.REVOKED_AT.isNull())
                .execute();
    }

    public int purgeSessionsEndedBefore(Instant cutoffTime) {
        return db.delete(ADMIN_USER_SESSIONS)
                .where(
                        // has been revoked, and revocation was more than (cutoff time) ago
                        ADMIN_USER_SESSIONS.REVOKED_AT.isNotNull()
                                .and(ADMIN_USER_SESSIONS.REVOKED_AT.lt(cutoffTime))
                )
                .or(
                        // not actively revoked, but the expiration was more than (cutoff time) ago
                        ADMIN_USER_SESSIONS.REVOKED_AT.isNull()
                                .and(ADMIN_USER_SESSIONS.EXPIRES_AT.lt(cutoffTime))
                )
                .execute();
    }
}
