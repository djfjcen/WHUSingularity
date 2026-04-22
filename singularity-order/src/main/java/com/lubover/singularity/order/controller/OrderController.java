package com.lubover.singularity.order.controller;

import com.lubover.singularity.api.Actor;
import com.lubover.singularity.api.Result;
import com.lubover.singularity.order.entity.Order;
import com.lubover.singularity.order.mapper.OrderMapper;
import com.lubover.singularity.order.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;
    private final OrderMapper orderMapper;

    public OrderController(OrderService orderService, OrderMapper orderMapper) {
        this.orderService = orderService;
        this.orderMapper = orderMapper;
    }

    @PostMapping("/snag")
    public Map<String, Object> snagOrder(@RequestBody Map<String, Object> request) {
        String userId = request.get("userId") == null ? null : String.valueOf(request.get("userId"));
        if (userId == null || userId.isBlank()) {
            return failure("userId is required");
        }

        Result result = orderService.snagOrder(new SimpleActor(userId));
        if (result.isSuccess()) {
            Map<String, Object> data = new HashMap<>();
            data.put("orderId", result.getMessage());
            return success(data);
        }
        return failure(result.getMessage());
    }

    @GetMapping("/{orderId}")
    public Map<String, Object> getOrderById(@PathVariable("orderId") String orderId) {
        Order order = orderMapper.selectByOrderId(orderId);
        if (order == null) {
            return failure("order not found");
        }
        return success(order);
    }

    @PutMapping("/{orderId}/status")
    public Map<String, Object> updateOrderStatus(@PathVariable("orderId") String orderId,
            @RequestBody Map<String, Object> request) {
        String status = request.get("status") == null ? null : String.valueOf(request.get("status"));
        if (status == null || status.isBlank()) {
            return failure("status is required");
        }
        int rows = orderMapper.updateStatus(orderId, status);
        if (rows <= 0) {
            return failure("order not found");
        }
        Order order = orderMapper.selectByOrderId(orderId);
        return success(order);
    }

    private Map<String, Object> success(Object data) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("data", data);
        return resp;
    }

    private Map<String, Object> failure(String message) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", false);
        resp.put("message", message);
        return resp;
    }

    private static final class SimpleActor implements Actor {
        private final String id;

        private SimpleActor(String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public Map<String, ?> getMetadata() {
            return Collections.emptyMap();
        }
    }
}
