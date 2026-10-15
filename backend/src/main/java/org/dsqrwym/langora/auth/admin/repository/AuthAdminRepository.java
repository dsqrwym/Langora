package org.dsqrwym.langora.auth.admin.repository;

import lombok.RequiredArgsConstructor;
import org.dsqrwym.langora.infrastructure.jooq.enums.AdminUserRole;
import org.dsqrwym.langora.infrastructure.jooq.enums.AdminUserStatus;
import org.jooq.DSLContext;
import org.jooq.Record6;
import org.jooq.SelectSelectStep;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

import static org.dsqrwym.langora.infrastructure.jooq.Tables.ADMIN_USERS;
import static org.jooq.Records.mapping;

@Repository
@RequiredArgsConstructor
public class AuthAdminRepository {
    private final DSLContext db;

    public record SelectedAdminUser(
            Long id, String username, String email, AdminUserRole role, String passwordHash, AdminUserStatus status
    ) {
    }

    private SelectSelectStep<Record6<Long, String, String, AdminUserRole, String, AdminUserStatus>> getBaseSelect() {
        return db.select(
                ADMIN_USERS.ID,
                ADMIN_USERS.USERNAME,
                ADMIN_USERS.EMAIL,
                ADMIN_USERS.ROLE,
                ADMIN_USERS.PASSWORD_HASH,
                ADMIN_USERS.STATUS
        );
    }

    public Optional<SelectedAdminUser> findByUsername(String username) {
        return getBaseSelect()
                .from(ADMIN_USERS)
                .where(ADMIN_USERS.USERNAME.eq(username))
                .fetchOptional(mapping(SelectedAdminUser::new));
    }

    public Optional<SelectedAdminUser> findByEmail(String email) {
        return getBaseSelect()
                .from(ADMIN_USERS)
                .where(ADMIN_USERS.EMAIL.eq(email))
                .fetchOptional(mapping(SelectedAdminUser::new));
    }

    public Optional<SelectedAdminUser> findByUsernameOrEmail(String username, String email) {
        if (username != null && !username.isBlank()) {
            return findByUsername(username);
        }
        return findByEmail(email);
    }

    public void setLastLoginAt(Long userId, Instant now) {
        db.update(ADMIN_USERS)
                .set(ADMIN_USERS.LAST_LOGIN_AT, now)
                .set(ADMIN_USERS.UPDATED_AT, now)
                .where(ADMIN_USERS.ID.eq(userId))
                .execute();
    }
}
