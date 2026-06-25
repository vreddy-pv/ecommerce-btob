package com.btob.catalog.service;

import com.btob.catalog.event.OrderCancelledEvent;
import com.btob.catalog.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * RabbitMQ consumer for order lifecycle events.
 * Processes OrderCreatedEvent to reserve inventory and OrderCancelledEvent to release it.
 * Per INVENTORY-01, INVENTORY-02.
 */
@Component
@Slf4j
public class OrderEventConsumer {

    private final CatalogService catalogService;

    /**
     * Tracks processed order IDs for idempotent processing.
     * Uses ConcurrentHashMap-backed Set for thread safety.
     */
    private final Set<UUID> processedOrderIds = ConcurrentHashMap.newKeySet();

    public OrderEventConsumer(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /**
     * Consumer for OrderCreatedEvent — reserves inventory for each item in the order.
     * Skips processing if the order ID has already been processed (idempotency).
     */
    @Bean
    public Consumer<OrderCreatedEvent> orderCreatedConsumer() {
        return event -> {
            if (event.getOrderId() == null) {
                log.warn("Received OrderCreatedEvent with null orderId — skipping");
                return;
            }

            if (!processedOrderIds.add(event.getOrderId())) {
                log.info("Order {} already processed — skipping duplicate OrderCreatedEvent", event.getOrderId());
                return;
            }

            log.info("Processing OrderCreatedEvent: orderId={}, accountId={}, items={}",
                    event.getOrderId(), event.getAccountId(), event.getItems() != null ? event.getItems().size() : 0);

            if (event.getItems() != null) {
                for (OrderCreatedEvent.OrderItemEvent item : event.getItems()) {
                    try {
                        catalogService.reserveInventory(item.getProductSku(), item.getQuantity());
                        log.debug("Reserved inventory: sku={}, quantity={}", item.getProductSku(), item.getQuantity());
                    } catch (Exception e) {
                        log.error("Failed to reserve inventory for SKU {} in order {}: {}",
                                item.getProductSku(), event.getOrderId(), e.getMessage());
                    }
                }
            }

            log.info("Successfully processed OrderCreatedEvent: orderId={}", event.getOrderId());
        };
    }

    /**
     * Consumer for OrderCancelledEvent — releases reserved inventory for each item.
     * Skips processing if the order ID has already been processed (idempotency).
     */
    @Bean
    public Consumer<OrderCancelledEvent> orderCancelledConsumer() {
        return event -> {
            if (event.getOrderId() == null) {
                log.warn("Received OrderCancelledEvent with null orderId — skipping");
                return;
            }

            if (!processedOrderIds.add(event.getOrderId())) {
                log.info("Order {} already processed — skipping duplicate OrderCancelledEvent", event.getOrderId());
                return;
            }

            log.info("Processing OrderCancelledEvent: orderId={}, accountId={}, reason={}, wasConfirmed={}",
                    event.getOrderId(), event.getAccountId(), event.getReason(), event.isWasConfirmed());

            if (event.getItems() != null) {
                for (OrderCancelledEvent.OrderItemEvent item : event.getItems()) {
                    try {
                        catalogService.releaseReservation(item.getProductSku(), item.getQuantity(), event.isWasConfirmed());
                        log.debug("Released reservation: sku={}, quantity={}, wasConfirmed={}",
                                item.getProductSku(), item.getQuantity(), event.isWasConfirmed());
                    } catch (Exception e) {
                        log.error("Failed to release reservation for SKU {} in order {}: {}",
                                item.getProductSku(), event.getOrderId(), e.getMessage());
                    }
                }
            }

            log.info("Successfully processed OrderCancelledEvent: orderId={}", event.getOrderId());
        };
    }
}
