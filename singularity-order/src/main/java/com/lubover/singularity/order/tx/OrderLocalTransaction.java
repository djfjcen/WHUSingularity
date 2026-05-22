package com.lubover.singularity.order.tx;

import com.lubover.singularity.order.registry.SlotRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 封装一次抢单请求的本地事务逻辑，由 handler 构造并通过 arg 传给 RocketMQ 事务监听器执行。
 */
public class OrderLocalTransaction {

    private static final String LUA_SCRIPT = """
            local stock = tonumber(redis.call('GET', KEYS[1]) or '0')
            if stock <= 0 then
                return -1
            end

            local remaining = redis.call('DECR', KEYS[1])
            if remaining < 0 then
                redis.call('INCR', KEYS[1])
                return -1
            end

            redis.call('HSET', KEYS[2],
                'orderId', ARGV[1],
                'actorId', ARGV[2],
                'slotId', ARGV[3],
                'productId', ARGV[4],
                'status', ARGV[5],
                'createTime', ARGV[6])

            return remaining
            """;

    private static final DefaultRedisScript<Long> LUA = new DefaultRedisScript<>(LUA_SCRIPT, Long.class);

    private static final Logger log = LoggerFactory.getLogger(OrderLocalTransaction.class);

    private final String orderId;
    private final String actorId;
    private final String slotId;
    private final String productId;
    private final String redisStockKey;
    private final StringRedisTemplate redisTemplate;
    private final SlotRegistry registry;
    private final LocalDateTime createTime;

    /** 最近一次 {@link #execute()} 的 Lua 返回值，失败或未执行时为 -1。 */
    private long lastRemaining = -1;

    public OrderLocalTransaction(String orderId, String actorId, String slotId,
            String productId,
            String redisStockKey,
            StringRedisTemplate redisTemplate,
            SlotRegistry registry,
            LocalDateTime createTime) {
        this.orderId = orderId;
        this.actorId = actorId;
        this.slotId = slotId;
        this.productId = productId;
        this.redisStockKey = redisStockKey;
        this.redisTemplate = redisTemplate;
        this.registry = registry;
        this.createTime = createTime;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getSlotId() {
        return slotId;
    }

    public long getLastRemaining() {
        return lastRemaining;
    }

    /**
     * @return true 表示成功（后续由监听器 COMMIT 半消息），false 表示失败（ROLLBACK）
     */
    public boolean execute() {
        lastRemaining = -1;
        try {
            String orderKey = "order:" + orderId;

            Long remaining = redisTemplate.execute(
                    LUA,
                    List.of(redisStockKey, orderKey),
                    orderId,
                    actorId,
                    slotId,
                    productId,
                    "1",
                    createTime.toString());

            if (remaining == null || remaining < 0) {
                log.warn("stock exhausted or lua failed: slot={} key={}", slotId, redisStockKey);
                return false;
            }

            lastRemaining = remaining;
            if (remaining == 0) {
                registry.markEmpty(slotId);
            }

            log.debug("local tx ok: orderId={} slot={} remaining={}", orderId, slotId, remaining);
            return true;

        } catch (Exception e) {
            log.error("local tx error: orderId={}", orderId, e);
            return false;
        }
    }
}
