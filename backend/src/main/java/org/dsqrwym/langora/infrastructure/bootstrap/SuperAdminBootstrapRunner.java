package org.dsqrwym.langora.infrastructure.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dsqrwym.langora.infrastructure.jooq.enums.AdminUserRole;
import org.dsqrwym.langora.infrastructure.jooq.enums.AdminUserStatus;
import org.dsqrwym.langora.infrastructure.jooq.tables.records.AdminUsersRecord;
import org.jooq.DSLContext;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;

import static org.dsqrwym.langora.infrastructure.jooq.Tables.ADMIN_USERS;

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class SuperAdminBootstrapRunner implements ApplicationRunner {
    private final DSLContext db;
    private final SuperAdminBootstrapProperties properties;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate tx;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        if (!properties.enabled()) {
            log.info("[Boostrap] Super admin bootstrap is disabled by configuration");
            return;
        }

        tx.executeWithoutResult(_ -> {

            AdminUsersRecord existingSuperAdmin = db.select(
                            ADMIN_USERS.ID, ADMIN_USERS.EMAIL, ADMIN_USERS.USERNAME
                    )
                    .from(ADMIN_USERS)
                    .where(ADMIN_USERS.ROLE.eq(AdminUserRole.SUPER_ADMIN))
                    .fetchOneInto(ADMIN_USERS);

            final Instant now = Instant.now();

            if (existingSuperAdmin == null) {

                boolean conflict = db.fetchExists(
                        db.selectOne()
                                .from(ADMIN_USERS)
                                .where(
                                        ADMIN_USERS.USERNAME.equal(properties.username())
                                                .or(ADMIN_USERS.EMAIL.equalIgnoreCase(properties.email()))
                                )
                );

                if (conflict) {
                    throw new IllegalStateException(
                            "Bootstrap username/email is already taken by a non-SUPER_ADMIN account. \n" +
                                    "Refusing to promote. Resolve manually."
                    );
                }

                log.info("[Bootstrap] No SUPER_ADMIN. Creating '{}'.", properties.username());

                db.insertInto(ADMIN_USERS,
                                ADMIN_USERS.EMAIL,
                                ADMIN_USERS.USERNAME,
                                ADMIN_USERS.PASSWORD_HASH,
                                ADMIN_USERS.ROLE,
                                ADMIN_USERS.STATUS,
                                ADMIN_USERS.CREATED_AT
                        )
                        .values(properties.email(),
                                properties.username(),
                                passwordEncoder.encode(properties.password()),
                                AdminUserRole.SUPER_ADMIN,
                                AdminUserStatus.ACTIVE,
                                now
                        )
                        .execute();
                log.info("[Bootstrap] Super admin bootstrap completed");
                return;
            }

            final Long id = existingSuperAdmin.getId();
            if (properties.overrideExistingPassword()) {
                log.warn("[Bootstrap] SUPER_ADMIN id={} exists. Overriding password.", id);
                db.update(ADMIN_USERS)
                        .set(ADMIN_USERS.PASSWORD_HASH, passwordEncoder.encode(properties.password()))
                        .set(ADMIN_USERS.STATUS, AdminUserStatus.ACTIVE)
                        .set(ADMIN_USERS.UPDATED_AT, now)
                        .where(ADMIN_USERS.ID.eq(id))
                        .execute();
            } else {
                log.info("[Bootstrap] SUPER_ADMIN id={}, username={}, email={} exists. Skipped.", id, existingSuperAdmin.getUsername(), existingSuperAdmin.getEmail());
            }
        });
    }
}
