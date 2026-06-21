package com.btob.catalog.controller;

import com.btob.catalog.dto.CategoryDto;
import com.btob.catalog.dto.ProductDto;
import com.btob.catalog.entity.AccountTier;
import com.btob.catalog.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for catalog operations.
 * Provides endpoints for product search, category browsing, and tier pricing.
 */
@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    /**
     * List products with optional search, category filter, and pagination.
     * GET /api/catalog/products?search=brake&categoryId={id}&page=0&size=20
     */
    @GetMapping("/products")
    public ResponseEntity<Page<ProductDto>> getProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ProductDto> products = catalogService.getProducts(search, categoryId, page, size);
        return ResponseEntity.ok(products);
    }

    /**
     * Get product by SKU with tier pricing.
     * GET /api/catalog/products/{sku}
     */
    @GetMapping("/products/{sku}")
    public ResponseEntity<ProductDto> getProductBySku(@PathVariable String sku) {
        ProductDto product = catalogService.getProductBySku(sku);
        return ResponseEntity.ok(product);
    }

    /**
     * Get tier-specific price for a product.
     * GET /api/catalog/products/{sku}/price?tier=GOLD
     */
    @GetMapping("/products/{sku}/price")
    public ResponseEntity<ProductDto> getProductPrice(
            @PathVariable String sku,
            @RequestParam AccountTier tier) {
        ProductDto product = catalogService.getProductWithTierPrice(sku, tier);
        return ResponseEntity.ok(product);
    }

    /**
     * List all categories with hierarchy.
     * GET /api/catalog/categories
     */
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryDto>> getCategories() {
        List<CategoryDto> categories = catalogService.getCategories();
        return ResponseEntity.ok(categories);
    }

    /**
     * Get category by ID.
     * GET /api/catalog/categories/{id}
     */
    @GetMapping("/categories/{id}")
    public ResponseEntity<CategoryDto> getCategoryById(@PathVariable UUID id) {
        CategoryDto category = catalogService.getCategoryById(id);
        return ResponseEntity.ok(category);
    }

    /**
     * Create new product (authenticated).
     * POST /api/catalog/products
     */
    @PostMapping("/products")
    public ResponseEntity<ProductDto> createProduct(@RequestBody ProductDto productDto) {
        ProductDto createdProduct = catalogService.createProduct(productDto);
        return ResponseEntity.ok(createdProduct);
    }

    /**
     * Update inventory level.
     * PUT /api/catalog/products/{sku}/inventory
     */
    @PutMapping("/products/{sku}/inventory")
    public ResponseEntity<Void> updateInventory(
            @PathVariable String sku,
            @RequestParam int quantity) {
        catalogService.updateInventory(sku, quantity);
        return ResponseEntity.ok().build();
    }
}