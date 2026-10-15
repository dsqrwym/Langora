package org.dsqrwym.langora.audit;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import static org.dsqrwym.langora.infrastructure.jooq.Tables.OPERATION_LOGS;

@Repository
@RequiredArgsConstructor
public class OperationLogRepository {
    private final DSLContext db;

    public void insert(OperationDto operation) {
        db.insertInto(OPERATION_LOGS)
                .set(OPERATION_LOGS.ACTION, operation.getAction())
                .set(OPERATION_LOGS.ACTOR_ID, operation.getActorId())
                .set(OPERATION_LOGS.ACTOR_TYPE, operation.getActorType())
                .set(OPERATION_LOGS.ACTOR_DISPLAY_NAME, operation.getActorDisplayName())
                .set(OPERATION_LOGS.RESOURCE_ID, operation.getResourceId())
                .set(OPERATION_LOGS.RESOURCE_TYPE, operation.getResourceType())
                .set(OPERATION_LOGS.RESOURCE_DISPLAY_NAME, operation.getResourceDisplayName())
                .set(OPERATION_LOGS.TARGET_ACTOR_ID, operation.getTargetId())
                .set(OPERATION_LOGS.TARGET_ACTOR_TYPE, operation.getTargetType())
                .set(OPERATION_LOGS.TARGET_ACTOR_DISPLAY_NAME, operation.getTargetDisplayName())
                .set(OPERATION_LOGS.BEFORE_DATA, operation.getBeforeData())
                .set(OPERATION_LOGS.AFTER_DATA, operation.getAfterData())
                .set(OPERATION_LOGS.USER_AGENT, operation.getUserAgent())
                .set(OPERATION_LOGS.IP_ADDRESS, operation.getIpAddress())
                .set(OPERATION_LOGS.CREATED_AT, operation.getCreatedAt())
                .execute();
    }
}
