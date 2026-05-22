package com.lubover.singularity.order.interceptor;

import com.lubover.singularity.api.Context;
import com.lubover.singularity.api.Interceptor;
import com.lubover.singularity.api.Result;
import com.lubover.singularity.order.metrics.OrderSnagMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * 抢单拦截器：始终上报 Micrometer；仅在非 loadtest 时打日志。
 */
@Component
public class LoggingInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);
    private static final String PATH_ALLOCATE = "allocate";

    private final OrderSnagMetrics metrics;
    private final boolean logEnabled;

    public LoggingInterceptor(OrderSnagMetrics metrics, Environment env) {
        this.metrics = metrics;
        this.logEnabled = !env.acceptsProfiles(Profiles.of("loadtest"));
    }

    @Override
    public void handle(Context context) {
        String actorId = context.getCurrActor().getId();
        String slotId = context.getCurrSlot().getId();
        if (logEnabled) {
            log.info("snag start: actor={} slot={}", actorId, slotId);
        }

        long start = System.nanoTime();
        context.next();
        long elapsed = System.nanoTime() - start;

        Result result = context.getResult();
        boolean success = result != null && result.isSuccess();
        metrics.recordInterceptor(elapsed, PATH_ALLOCATE, slotId, success);

        if (!logEnabled) {
            return;
        }
        long costMs = elapsed / 1_000_000;
        if (success) {
            log.info("snag success: actor={} slot={} orderId={} cost={}ms",
                    actorId, slotId, result.getMessage(), costMs);
        } else {
            log.warn("snag failed: actor={} slot={} reason={} cost={}ms",
                    actorId, slotId, result != null ? result.getMessage() : "no result", costMs);
        }
    }
}
