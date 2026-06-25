package com.btob.order.service;

import com.btob.order.event.OrderCancelledEvent;
import com.btob.order.event.OrderCreatedEvent;
import com.btob.order.event.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventPublisher {

    private final StreamBridge streamBridge;

    public void publishOrderCreated(OrderCreatedEvent event) {
        log.info("Publishing OrderCreatedEvent for order: {}", event.getOrderId());
        streamBridge.send("orderCreated-out-0", event);
    }

    public void publishOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("Publishing OrderStatusChangedEvent for order: {} - {} -> {}",
                event.getOrderId(), event.getOldStatus(), event.getNewStatus());
        streamBridge.send("orderStatusChanged-out-0", event);
    }

    public void publishOrderCancelled(OrderCancelledEvent event) {
        log.info("Publishing OrderCancelledEvent for order: {}", event.getOrderId());
        streamBridge.send("orderCancelled-out-0", event);
    }
}
