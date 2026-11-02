package org.dsqrwym.langora.audit.auth;

import org.dsqrwym.langora.audit.OperationDto;
import org.dsqrwym.langora.infrastructure.jooq.enums.AdminSessionRevocationReason;
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
     * 为管理员登录创建审计事件：actor 是登录者，resource 是本次创建的会话。
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
     * Records an automatic revocation of every active session of an account.
     * Used when a rotated refresh token is replayed (possible theft) or when a
     * disabled account still presents a usable refresh token.
     *
     * 记录一次“自动撤销账号全部会话”的安全事件；用于刷新 token 重放
     * 或停用账号仍持有可用 refresh token 的场景。
     *
     * @param userId                 ID of the account whose sessions were revoked
     * @param username               username of the affected account
     * @param triggerSessionId       session that triggered the revocation
     * @param triggerDeviceName      device name of the triggering session
     * @param reason                 why the sessions were revoked
     * @param revokedSessionCount    number of sessions revoked in this operation
     * @param secondsSinceLastActive time since the session was last active,
     *                               or {@code null} when not applicable
     * @param ipAddress              IP of the request that triggered the revocation
     * @param userAgent              User-Agent of the request that triggered the revocation
     * @param detectedAt             timestamp of the detection
     */
    public static @NonNull OperationDto sessionsRevoked(
            Long userId,
            String username,
            UUID triggerSessionId,
            String triggerDeviceName,
            AdminSessionRevocationReason reason,
            int revokedSessionCount,
            Long secondsSinceLastActive,
            String ipAddress,
            String userAgent,
            Instant detectedAt
    ) {
        OperationDto dto = OperationDto.system(
                OperationLogAction.REVOKE_SESSION,
                OperationLogResourceType.ADMIN_USER_SESSION,
                detectedAt
        );

        dto.setResourceId(triggerSessionId.toString());
        dto.setResourceDisplayName(triggerDeviceName);

        dto.setTargetType(OperationLogActorType.ADMIN);
        dto.setTargetId(userId.toString());
        dto.setTargetDisplayName(username);

        dto.setAfterData(org.jooq.JSONB.jsonb("""
                        {
                          "reason": "%s",
                          "revokedSessionCount": %d,
                          "secondsSinceLastActive": %s
                        }
                        """.formatted(
                        reason.name(),
                        revokedSessionCount,
                        secondsSinceLastActive == null ? "null" : secondsSinceLastActive.toString()
                )
        ));

        dto.setIpAddress(ipAddress);
        dto.setUserAgent(userAgent);

        return dto;
    }

    /**
     * Creates an audit event for an administrator logout.
     *
     * 为管理员登出创建审计事件。
     *
     * @param userId     ID of the administrator who logged out
     * @param username   Username of the administrator
     * @param sessionId  ID of the session that was revoked
     * @param ipAddress  IP of the logout request
     * @param userAgent  User-Agent of the logout request
     * @param logoutTime Timestamp of the logout
     */
    public static @NonNull OperationDto adminLogout(
            Long userId, String username, UUID sessionId,
            String ipAddress, String userAgent,
            Instant logoutTime
    ) {
        OperationDto dto = new OperationDto(
                OperationLogActorType.ADMIN,
                OperationLogAction.LOGOUT,
                OperationLogResourceType.ADMIN_USER_SESSION,
                logoutTime
        );

        dto.setActorId(userId.toString());
        dto.setActorDisplayName(username);
        dto.setResourceId(sessionId.toString());
        dto.setIpAddress(ipAddress);
        dto.setUserAgent(userAgent);

        return dto;
    }

}
