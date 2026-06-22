package com.btob.catalog.mcp;

import com.btob.catalog.dto.ProductDto;
import com.btob.catalog.dto.TierPricingDto;
import com.btob.catalog.entity.AccountTier;
import com.btob.catalog.service.CatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogMcpToolsTest {

    @Mock
    private CatalogService catalogService;

    private CatalogMcpTools catalogMcpTools;

    @BeforeEach
    void setUp() {
        catalogMcpTools = new CatalogMcpTools(catalogService);
    }

    @Test
    void searchProductsReturnsResults() {
        ProductDto product = ProductDto.builder()
                .id(UUID.randomUUID())
                .sku("BRK-001")
                .name("Front Brake Pads")
                .description("High-quality front brake pads")
                .basePrice(new BigDecimal("29.99"))
                .inventoryLevel(100)
                .categoryName("Brakes")
                .isActive(true)
                .build();

        Page<ProductDto> page = new PageImpl<>(List.of(product));
        when(catalogService.getProducts(anyString(), any(), anyInt(), anyInt())).thenReturn(page);

        List<Map<String, Object>> results = catalogMcpTools.search_products("brake", null);

        assertThat(results).hasSize(1);
        Map<String, Object> result = results.get(0);
        assertThat(result)
                .containsEntry("sku", "BRK-001")
                .containsEntry("name", "Front Brake Pads")
                .containsEntry("basePrice", new BigDecimal("29.99"))
                .containsEntry("inventoryLevel", 100)
                .containsEntry("isActive", true);
    }

    @Test
    void searchProductsHandlesException() {
        when(catalogService.getProducts(anyString(), any(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("DB error"));

        List<Map<String, Object>> results = catalogMcpTools.search_products("error", null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0)).containsKey("error");
    }

    @Test
    void getProductBySkuReturnsProduct() {
        ProductDto product = ProductDto.builder()
                .id(UUID.randomUUID())
                .sku("ELC-001")
                .name("Car Battery")
                .description("12V car battery")
                .basePrice(new BigDecimal("89.99"))
                .inventoryLevel(60)
                .categoryName("Electrical")
                .isActive(true)
                .tierPricing(List.of(
                        TierPricingDto.builder()
                                .tier(AccountTier.SILVER)
                                .price(new BigDecimal("84.99"))
                                .build()
                ))
                .build();

        when(catalogService.getProductBySku("ELC-001")).thenReturn(product);

        Map<String, Object> result = catalogMcpTools.get_product_by_sku("ELC-001");

        assertThat(result)
                .containsEntry("sku", "ELC-001")
                .containsEntry("name", "Car Battery")
                .containsEntry("basePrice", new BigDecimal("89.99"))
                .containsEntry("inventoryLevel", 60);
    }

    @Test
    void getProductBySkuHandlesNotFound() {
        String sku = "INVALID";
        when(catalogService.getProductBySku(sku))
                .thenThrow(new com.btob.catalog.exception.ResourceNotFoundException("Product not found with SKU: " + sku));

        Map<String, Object> result = catalogMcpTools.get_product_by_sku(sku);

        assertThat(result).containsEntry("error", "Product not found: " + sku);
    }

    @Test
    void getProductBySkuHandlesException() {
        when(catalogService.getProductBySku("ERROR"))
                .thenThrow(new RuntimeException("Unexpected error"));

        Map<String, Object> result = catalogMcpTools.get_product_by_sku("ERROR");

        assertThat(result).containsEntry("error", "Product not found: ERROR");
    }
}
