package com.btob.order.service;

import com.btob.order.event.OrderCreatedEvent;
import com.btob.order.event.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

/**
 * Publishes order events via Spring Cloud Stream.
 * Per D-03, D-04: Event-driven messaging via Spring Cloud Stream.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventPublisher {

    private final StreamBridge streamBridge;

    /**
     * Publish an order created event.
     */
    public void publishOrderCreated(OrderCreatedEvent event) {
        log.info("Publishing OrderCreatedEvent for order: {}", event.getOrderId());
        streamBridge.send("orderCreated-out-0", event);
    }

    /**
     * Publish an order status changed event.
     */
    public void publishOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("Publishing OrderStatusChangedEvent for order: {} - {} -> {}",
                event.getOrderId(), event.getOldStatus(), event.getNewStatus());
        streamBridge.send("orderStatusChanged-out-0", event);
    }
}
