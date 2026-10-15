package org.dsqrwym.langora.audit;

import lombok.Data;
import org.dsqrwym.langora.infrastructure.jooq.enums.OperationLogAction;
import org.dsqrwym.langora.infrastructure.jooq.enums.OperationLogActorType;
import org.dsqrwym.langora.infrastructure.jooq.enums.OperationLogResourceType;
import org.jooq.JSONB;

import java.time.Instant;

@Data
public class OperationDto {
    private OperationLogActorType actorType;
    private String actorId;
    private String actorDisplayName;
    private OperationLogAction action;
    private OperationLogResourceType resourceType;
    private String resourceId;
    private String resourceDisplayName;
    private OperationLogActorType targetType;
    private String targetId;
    private String targetDisplayName;
    private JSONB beforeData;
    private JSONB afterData;
    private String ipAddress;
    private String userAgent;
    private Instant createdAt;

    public OperationDto(
            OperationLogActorType actorType,
            OperationLogAction action,
            OperationLogResourceType resourceType,
            Instant createdAt
    ) {
        this.actorType = actorType;
        this.action = action;
        this.resourceType = resourceType;
        this.createdAt = createdAt;
    }

    public static OperationDto system(
            OperationLogAction action,
            OperationLogResourceType resourceType,
            Instant createdAt
    ) {
        return new OperationDto(
                OperationLogActorType.SYSTEM,
                action,
                resourceType,
                createdAt
        );
    }
}
