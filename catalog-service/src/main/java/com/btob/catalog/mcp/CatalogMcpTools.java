package com.btob.catalog.mcp;

import com.btob.catalog.dto.ProductDto;
import com.btob.catalog.service.CatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * MCP Tools for catalog management.
 * Exposes catalog operations to AI agents via MCP protocol.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CatalogMcpTools {

    private final CatalogService catalogService;

    /**
     * Search for products by query and optional category.
     *
     * @param query Search term (product name or SKU)
     * @param category Optional category filter
     * @return List of products with SKU, name, price, and inventory
     */
    @Tool(description = "Search for auto parts products by name or SKU. Optionally filter by category. Returns list of products with SKU, name, price, and inventory levels.")
    public List<Map<String, Object>> search_products(
            @ToolParam(description = "Search term (product name or SKU)") String query,
            @ToolParam(description = "Optional category filter", required = false) String category) {

        log.info("MCP Tool called: search_products(query={}, category={})", query, category);

        try {
            Page<ProductDto> products = catalogService.getProducts(query, null, 0, 20);

            return products.getContent().stream()
                    .map(this::productToMap)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error searching products", e);
            return List.of(Map.of("error", "Failed to search products: " + e.getMessage()));
        }
    }

    /**
     * Get product details by SKU.
     *
     * @param sku Product SKU
     * @return Product details with tier pricing
     */
    @Tool(description = "Get detailed product information by SKU. Returns product name, description, price, inventory, and tier pricing.")
    public Map<String, Object> get_product_by_sku(
            @ToolParam(description = "Product SKU (e.g., BRK-001, ELC-002)") String sku) {

        log.info("MCP Tool called: get_product_by_sku({})", sku);

        try {
            ProductDto product = catalogService.getProductBySku(sku);
            return productToMap(product);
        } catch (Exception e) {
            log.error("Error getting product by SKU", e);
            return Map.of("error", "Product not found: " + sku);
        }
    }

    /**
     * Get list of products that are low on stock.
     * Available inventory at or below the reorder point.
     *
     * @return List of low-stock products with SKU, name, available, and reorder point
     */
    @Tool(description = "Get list of products that are low on stock (available inventory at or below reorder point).")
    public List<Map<String, Object>> get_low_stock_items() {
        log.info("MCP Tool called: get_low_stock_items");

        try {
            List<ProductDto> lowStockProducts = catalogService.getLowStockProducts();

            return lowStockProducts.stream()
                    .map(p -> Map.<String, Object>of(
                            "sku", p.getSku(),
                            "name", p.getName(),
                            "available", p.getInventoryLevel() - (p.getReservedInventory() != null ? p.getReservedInventory() : 0),
                            "reorderPoint", p.getReorderPoint() != null ? p.getReorderPoint() : 0
                    ))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting low stock items", e);
            return List.of(Map.of("error", "Failed to get low stock items: " + e.getMessage()));
        }
    }

    /**
     * Check available stock for a product SKU.
     *
     * @param sku Product SKU
     * @return Map with SKU and available quantity
     */
    @Tool(description = "Check available stock for a product SKU. Returns current available quantity.")
    public Map<String, Object> check_stock(
            @ToolParam(description = "Product SKU (e.g., BRK-001, ELC-002)") String sku) {

        log.info("MCP Tool called: check_stock(sku={})", sku);

        try {
            int available = catalogService.getAvailableStock(sku);
            return Map.of("sku", sku, "available", available);
        } catch (Exception e) {
            log.error("Error checking stock for SKU: {}", sku, e);
            return Map.of("error", "Failed to check stock: " + e.getMessage());
        }
    }

    /**
     * Convert ProductDto to Map for MCP response.
     */
    private Map<String, Object> productToMap(ProductDto product) {
        Map<String, Object> map = new HashMap<>();
        map.put("sku", product.getSku());
        map.put("name", product.getName());
        map.put("description", product.getDescription());
        map.put("category", product.getCategoryName());
        map.put("basePrice", product.getBasePrice());
        map.put("inventoryLevel", product.getInventoryLevel());
        map.put("isActive", product.getIsActive());

        if (product.getTierPricing() != null && !product.getTierPricing().isEmpty()) {
            map.put("tierPricing", product.getTierPricing().stream()
                    .map(tier -> Map.of(
                            "tier", tier.getTier().name(),
                            "price", tier.getPrice()
                    ))
                    .collect(Collectors.toList()));
        }

        return map;
    }
}
