package org.dsqrwym.langora.infrastructure.jooqcustom;

import lombok.extern.slf4j.Slf4j;
import org.jooq.ExecuteContext;
import org.jooq.ExecuteListener;

@Slf4j
public class SqlExecutionLoggerListener implements ExecuteListener {
    private static final String START_TIME_KEY = "SQL_START_TIME";

    @Override
    public void executeStart(ExecuteContext ctx) {
        if (!log.isDebugEnabled()) return;
        ctx.data(START_TIME_KEY, System.nanoTime());
    }

    @Override
    public void executeEnd(ExecuteContext ctx) {
        if (!log.isDebugEnabled()) return;
        Long startTime = (Long) ctx.data(START_TIME_KEY);
        if (startTime != null) {
            long durationNanos = System.nanoTime() - startTime;
            double durationMillis = durationNanos / 1_000_000.0;

            log.debug("SQL execution took {} ms for SQL: {}", durationMillis, ctx.sql());
        }
    }
}
