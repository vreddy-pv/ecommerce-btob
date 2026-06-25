package com.btob.catalog.repository;

import com.btob.catalog.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Product entity with search/filter capabilities.
 * Satisfies CATALOG-01 (product queries) and CATALOG-02 (search/filter).
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    /**
     * Find product by SKU (unique identifier).
     * Used for direct product lookup per CATALOG-01.
     */
    Optional<Product> findBySku(String sku);

    /**
     * Search products by name (case-insensitive).
     * Used for text search per CATALOG-02.
     */
    List<Product> findByNameContainingIgnoreCase(String name);

    /**
     * Find products by category.
     * Used for category filtering per CATALOG-02.
     */
    List<Product> findByCategoryId(UUID categoryId);

    /**
     * Find all active products.
     * Used for catalog listing.
     */
    List<Product> findByIsActiveTrue();

    /**
     * Combined search by SKU or name.
     * Used for general search functionality.
     */
    List<Product> findBySkuOrNameContainingIgnoreCase(String sku, String name);

    /**
     * Search products with pagination.
     * Used for paginated catalog browsing.
     */
    Page<Product> findByIsActiveTrue(Pageable pageable);

    /**
     * Search products by category with pagination.
     */
    Page<Product> findByCategoryIdAndIsActiveTrue(UUID categoryId, Pageable pageable);

    /**
     * Search products by name with pagination.
     */
    Page<Product> findByNameContainingIgnoreCaseAndIsActiveTrue(String name, Pageable pageable);

    /**
     * Complex search combining multiple criteria.
     * Uses JPQL for flexible filtering.
     */
    @Query("SELECT p FROM Product p WHERE " +
           "(:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "p.isActive = true")
    Page<Product> searchProducts(
            @Param("search") String search,
            @Param("categoryId") UUID categoryId,
            Pageable pageable);

    /**
     * Find product by SKU with pessimistic write lock.
     * Used for high-contention inventory reservation paths.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.sku = :sku")
    Optional<Product> findBySkuForUpdate(@Param("sku") String sku);

    /**
     * Find products where available stock (inventoryLevel - reservedInventory)
     * is at or below the reorder point.
     * Used for low-stock alerting per INV-05.
     */
    @Query("SELECT p FROM Product p WHERE (p.inventoryLevel - p.reservedInventory) <= p.reorderPoint")
    List<Product> findLowStockProducts();

    /**
     * Find products with reorder point at or above the given threshold.
     * Used for filtering products that need attention.
     */
    List<Product> findByReorderPointGreaterThanEqual(Integer reorderPoint);
}