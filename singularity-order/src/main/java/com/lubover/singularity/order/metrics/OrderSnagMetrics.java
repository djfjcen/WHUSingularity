package com.lubover.singularity.order.metrics;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/** 抢单链路指标，供 {@link com.lubover.singularity.order.interceptor.LoggingInterceptor} 等复用。 */
@Component
public class OrderSnagMetrics {

    private static final double[] PERCENTILES = {0.5, 0.95, 0.99};

    private final MeterRegistry registry;
    private final Timer interceptorTimer;
    private final Timer handlerTimer;
    private final Timer txSendTimer;
    private final Timer localTxTimer;
    private final DistributionSummary remainingSummary;

    public OrderSnagMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.interceptorTimer = timer("order.snag.interceptor");
        this.handlerTimer = timer("order.snag.handler");
        this.txSendTimer = timer("order.snag.tx_send");
        this.localTxTimer = timer("order.snag.local_tx");
        this.remainingSummary = DistributionSummary.builder("order.snag.remaining")
                .publishPercentiles(PERCENTILES)
                .register(registry);
    }

    private Timer timer(String name) {
        return Timer.builder(name).publishPercentiles(PERCENTILES).register(registry);
    }

    public void recordInterceptor(long durationNanos, String path, String slotId, boolean success) {
        interceptorTimer.record(durationNanos, TimeUnit.NANOSECONDS);
        recordOutcome(path, slotId, success);
    }

    public void recordHandler(long durationNanos) {
        handlerTimer.record(durationNanos, TimeUnit.NANOSECONDS);
    }

    public void recordTxSend(long durationNanos) {
        txSendTimer.record(durationNanos, TimeUnit.NANOSECONDS);
    }

    public void recordLocalTx(String slotId, long durationNanos, boolean success, long remaining) {
        localTxTimer.record(durationNanos, TimeUnit.NANOSECONDS);
        registry.counter("order.snag.local_tx.outcome",
                "slot", slotId, "result", success ? "ok" : "fail").increment();
        if (success && remaining >= 0) {
            remainingSummary.record(remaining);
        }
    }

    public void recordTxState(String path, TransactionSendResult sendResult) {
        String state = sendResult != null && sendResult.getLocalTransactionState() != null
                ? sendResult.getLocalTransactionState().name() : "unknown";
        registry.counter("order.snag.tx.state", "path", path, "state", state).increment();
    }

    public void recordTxCheck(LocalTransactionState state) {
        registry.counter("order.snag.tx.check",
                "state", state != null ? state.name() : "null").increment();
    }

    public void recordOutcome(String path, String slotId, boolean success) {
        registry.counter("order.snag.outcome",
                "path", path, "slot", slotId, "result", success ? "success" : "fail").increment();
    }
}
