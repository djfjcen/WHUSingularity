package com.lubover.singularity.order.listener;

import com.lubover.singularity.order.metrics.OrderSnagMetrics;
import com.lubover.singularity.order.tx.OrderLocalTransaction;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 事务消息适配器，不包含任何业务逻辑。
 */
@Component
@RocketMQTransactionListener
public class OrderTransactionListener implements RocketMQLocalTransactionListener {

    private static final Logger log = LoggerFactory.getLogger(OrderTransactionListener.class);

    private final StringRedisTemplate redisTemplate;
    private final OrderSnagMetrics snagMetrics;

    public OrderTransactionListener(StringRedisTemplate redisTemplate, OrderSnagMetrics snagMetrics) {
        this.redisTemplate = redisTemplate;
        this.snagMetrics = snagMetrics;
    }

    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        if (!(arg instanceof OrderLocalTransaction localTx)) {
            log.error("unexpected arg type: {}", arg == null ? "null" : arg.getClass());
            return RocketMQLocalTransactionState.ROLLBACK;
        }
        long start = System.nanoTime();
        boolean ok = localTx.execute();
        snagMetrics.recordLocalTx(localTx.getSlotId(), System.nanoTime() - start, ok, localTx.getLastRemaining());
        return ok ? RocketMQLocalTransactionState.COMMIT : RocketMQLocalTransactionState.ROLLBACK;
    }

    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        String orderId = (String) msg.getHeaders().get("orderId");
        Boolean exists = redisTemplate.hasKey("order:" + orderId);
        RocketMQLocalTransactionState state = Boolean.TRUE.equals(exists)
                ? RocketMQLocalTransactionState.COMMIT
                : RocketMQLocalTransactionState.ROLLBACK;
        LocalTransactionState mapped = state == RocketMQLocalTransactionState.COMMIT
                ? LocalTransactionState.COMMIT_MESSAGE
                : LocalTransactionState.ROLLBACK_MESSAGE;
        snagMetrics.recordTxCheck(mapped);
        log.info("check tx callback: orderId={} -> {}", orderId, state);
        return state;
    }
}
