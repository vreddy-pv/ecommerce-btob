package com.btob.catalog.repository;

import com.btob.catalog.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Category entity with hierarchy support.
 * Supports nested categories for auto parts organization.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    /**
     * Search categories by name (case-insensitive).
     */
    List<Category> findByNameContainingIgnoreCase(String name);

    /**
     * Find root categories (no parent).
     * Used for top-level category listing.
     */
    List<Category> findByParentIdIsNull();

    /**
     * Find child categories by parent ID.
     * Used for subcategory navigation.
     */
    List<Category> findByParentId(UUID parentId);

    /**
     * Find root categories sorted by sort order.
     */
    List<Category> findByParentIdIsNullOrderBySortOrderAsc();

    /**
     * Find child categories sorted by sort order.
     */
    List<Category> findByParentIdOrderBySortOrderAsc(UUID parentId);
}