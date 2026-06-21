package com.btob.catalog.dto;

import com.btob.catalog.entity.AccountTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Data Transfer Object for TierPricing entity.
 * Represents tier-specific pricing for a product.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TierPricingDto {

    private UUID id;
    private UUID productId;
    private AccountTier tier;
    private BigDecimal price;
}