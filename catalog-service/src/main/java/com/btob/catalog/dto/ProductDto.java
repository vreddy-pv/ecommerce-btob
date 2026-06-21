package com.btob.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object for Product entity.
 * Includes tier pricing for frontend display.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {

    private UUID id;
    private String sku;
    private String name;
    private String description;
    private BigDecimal basePrice;
    private Integer inventoryLevel;
    private UUID categoryId;
    private String categoryName;
    private Boolean isActive;
    private List<TierPricingDto> tierPricing;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}