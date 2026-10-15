package org.dsqrwym.langora.auth.admin.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.dsqrwym.langora.infrastructure.jooq.enums.AdminUserRole;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Valid
@ConfigurationProperties(prefix = "langora.auth.admin.session")
public record AdminSessionProperties(
        @Positive Integer superAdminMaxSession,
        @Positive Integer adminMaxSession,
        @Positive Integer editorMaxSession,
        @Min(0)
        Integer refreshGraceSeconds
) {
    public AdminSessionProperties {
        if (superAdminMaxSession == null) superAdminMaxSession = 3;
        if (adminMaxSession == null) adminMaxSession = 5;
        if (editorMaxSession == null) editorMaxSession = 5;
        if (refreshGraceSeconds == null) refreshGraceSeconds = 5;
    }

    public Integer getMaxSessionByRole(AdminUserRole role) {
        return switch (role) {
            case SUPER_ADMIN -> superAdminMaxSession;
            case ADMIN -> adminMaxSession;
            case EDITOR -> editorMaxSession;
            default -> throw new IllegalArgumentException("Invalid role");
        };
    }
}
