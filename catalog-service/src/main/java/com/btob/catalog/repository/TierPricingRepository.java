package com.btob.catalog.repository;

import com.btob.catalog.entity.AccountTier;
import com.btob.catalog.entity.TierPricing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for TierPricing entity.
 * Supports tier-based pricing per CATALOG-03.
 */
@Repository
public interface TierPricingRepository extends JpaRepository<TierPricing, UUID> {

    /**
     * Find tier pricing for a specific product and tier.
     * Used for price lookup per CATALOG-03.
     */
    Optional<TierPricing> findByProductIdAndTier(UUID productId, AccountTier tier);

    /**
     * Find all tier pricing for a product.
     * Used for displaying all available tiers.
     */
    List<TierPricing> findByProductId(UUID productId);

    /**
     * Find tier pricing by product SKU and tier.
     * Used for combined lookup.
     */
    @Query("SELECT tp FROM TierPricing tp WHERE tp.product.sku = :sku AND tp.tier = :tier")
    Optional<TierPricing> findByProductSkuAndTier(@Param("sku") String sku,
                                                   @Param("tier") AccountTier tier);

    /**
     * Find all tier pricing for a product by SKU.
     */
    @Query("SELECT tp FROM TierPricing tp WHERE tp.product.sku = :sku")
    List<TierPricing> findByProductSku(@Param("sku") String sku);
}