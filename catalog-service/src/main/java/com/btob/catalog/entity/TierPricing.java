package com.btob.catalog.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Tier-based pricing for products.
 * Satisfies CATALOG-03 (tier-based pricing).
 * Each product can have different prices per account tier.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "tier_pricing",
    uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "tier"})
)
public class TierPricing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountTier tier;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
}
