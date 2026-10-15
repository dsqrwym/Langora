package org.dsqrwym.langora.audit.auth;

import org.dsqrwym.langora.audit.OperationDto;
import org.dsqrwym.langora.infrastructure.jooq.enums.OperationLogAction;
import org.dsqrwym.langora.infrastructure.jooq.enums.OperationLogActorType;
import org.dsqrwym.langora.infrastructure.jooq.enums.OperationLogResourceType;
import org.jspecify.annotations.NonNull;

import java.time.Instant;
import java.util.UUID;

public class AuthAuditEvents {
    private AuthAuditEvents() {
    }

    /**
     * Creates an audit event for an administrator login.
     * The actor is the administrator who logged in, and the resource
     * is the session created for the login.
     *
     * @param userId     ID of the administrator who logged in, used as the actor ID
     * @param username   Username of the administrator, used as the actor display name
     * @param sessionId  ID of the login session, used as the resource ID
     * @param deviceName Name of the device used for the login, used as the resource display name
     * @param ipAddress  IP address from which the login request originated
     * @param userAgent  User-Agent header of the login request
     * @param loginTime  Timestamp when the login occurred
     */
    public static @NonNull OperationDto adminLogin(
            Long userId, String username, UUID sessionId,
            String deviceName, String ipAddress, String userAgent,
            Instant loginTime
    ) {
        OperationDto dto = new OperationDto(
                OperationLogActorType.ADMIN,
                OperationLogAction.LOGIN,
                OperationLogResourceType.ADMIN_USER_SESSION,
                loginTime
        );

        dto.setActorId(userId.toString());
        dto.setActorDisplayName(username);
        dto.setResourceId(sessionId.toString());
        dto.setResourceDisplayName(deviceName);
        dto.setIpAddress(ipAddress);
        dto.setUserAgent(userAgent);

        return dto;
    }

    /**
     * Detects refresh token reuse outside the grace period.
     * The actor is SYSTEM because the reuse is detected automatically,
     * and the target is the account potentially compromised.
     *
     * @param victimUserId            ID of the potentially compromised account
     * @param victimUsername          Username of the affected account, used as the target display name
     * @param reusedSessionId         ID of the reused session, used as the resource ID
     * @param reusedSessionDeviceName Device name of the reused session, used as the resource display name
     * @param revokedSessionCount     Number of sessions revoked in this operation
     *                                (all active sessions belonging to the affected user)
     * @param secondsSinceLastActive  Number of seconds by which the reuse occurred after the last active;
     *                                useful for distinguishing an actual attack from a network retry or replay
     * @param attackerIp              IP address from which the reuse request originated
     * @param attackerUserAgent       User-Agent header of the request that triggered the detection
     * @param detectedAt              Timestamp when the reuse was detected
     */
    public static OperationDto tokenReuseDetected(
            Long victimUserId,
            String victimUsername,
            UUID reusedSessionId,
            String reusedSessionDeviceName,
            int revokedSessionCount,
            long secondsSinceLastActive,
            String attackerIp,
            String attackerUserAgent,
            Instant detectedAt
    ) {
        OperationDto dto = OperationDto.system(
                OperationLogAction.REVOKE_SESSION,
                OperationLogResourceType.ADMIN_USER_SESSION,
                detectedAt
        );

        dto.setResourceId(reusedSessionId.toString());
        dto.setResourceDisplayName(reusedSessionDeviceName);

        dto.setTargetType(OperationLogActorType.ADMIN);
        dto.setTargetId(victimUserId.toString());
        dto.setTargetDisplayName(victimUsername);

        dto.setAfterData(org.jooq.JSONB.jsonb("""
                {
                  "reason": "TOKEN_REUSE_DETECTED",
                  "revokedSessionCount": %d,
                  "secondsSinceLastActive": %d
                }
                """.formatted(revokedSessionCount, secondsSinceLastActive)));

        dto.setIpAddress(attackerIp);
        dto.setUserAgent(attackerUserAgent);

        return dto;
    }

}
