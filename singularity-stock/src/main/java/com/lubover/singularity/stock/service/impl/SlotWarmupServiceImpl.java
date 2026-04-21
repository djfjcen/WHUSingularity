package com.lubover.singularity.stock.service.impl;

import com.lubover.singularity.stock.dto.SlotPreheatResponse;
import com.lubover.singularity.stock.service.SlotWarmupService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class SlotWarmupServiceImpl implements SlotWarmupService {

    private static final String STOCK_KEY_PREFIX = "stock:";

    private final StringRedisTemplate redisTemplate;

    public SlotWarmupServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public SlotPreheatResponse preheatSlot(String slotId, Long quantity, boolean overwrite) {
        validate(slotId, quantity);

        String redisKey = STOCK_KEY_PREFIX + slotId;
        String value = String.valueOf(quantity);

        if (overwrite) {
            redisTemplate.opsForValue().set(redisKey, value);
            return new SlotPreheatResponse(redisKey, true, value, "slot已覆盖预热");
        }

        Boolean written = redisTemplate.opsForValue().setIfAbsent(redisKey, value);
        if (Boolean.TRUE.equals(written)) {
            return new SlotPreheatResponse(redisKey, true, value, "slot预热成功");
        }

        String currentValue = redisTemplate.opsForValue().get(redisKey);
        return new SlotPreheatResponse(redisKey, false, currentValue, "slot已存在，未覆盖");
    }

    private void validate(String slotId, Long quantity) {
        if (slotId == null || slotId.isBlank()) {
            throw new IllegalArgumentException("slotId不能为空");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("quantity必须大于0");
        }
    }
}
