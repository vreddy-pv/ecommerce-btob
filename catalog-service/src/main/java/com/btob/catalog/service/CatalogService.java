package com.btob.catalog.service;

import com.btob.catalog.dto.CategoryDto;
import com.btob.catalog.dto.ProductDto;
import com.btob.catalog.dto.TierPricingDto;
import com.btob.catalog.entity.AccountTier;
import com.btob.catalog.entity.Category;
import com.btob.catalog.entity.Product;
import com.btob.catalog.entity.TierPricing;
import com.btob.catalog.exception.ResourceNotFoundException;
import com.btob.catalog.repository.CategoryRepository;
import com.btob.catalog.repository.ProductRepository;
import com.btob.catalog.repository.TierPricingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Catalog business logic service.
 * Provides product search, category browsing, and tier pricing per CATALOG-01, CATALOG-02, CATALOG-03.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final TierPricingRepository tierPricingRepository;

    /**
     * Get paginated product list with optional search/filter.
     * @param search Optional search term (name or SKU)
     * @param categoryId Optional category ID filter
     * @param page Page number (0-based)
     * @param size Page size
     * @return Page of ProductDto
     */
    public Page<ProductDto> getProducts(String search, UUID categoryId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<Product> products;
        
        if (search != null && !search.isEmpty() && categoryId != null) {
            products = productRepository.searchProducts(search, categoryId, pageable);
        } else if (search != null && !search.isEmpty()) {
            products = productRepository.searchProducts(search, null, pageable);
        } else if (categoryId != null) {
            products = productRepository.findByCategoryIdAndIsActiveTrue(categoryId, pageable);
        } else {
            products = productRepository.findByIsActiveTrue(pageable);
        }
        
        return products.map(this::convertToDto);
    }

    /**
     * Get product by SKU with tier pricing.
     * @param sku Product SKU
     * @return ProductDto with tier pricing
     */
    public ProductDto getProductBySku(String sku) {
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with SKU: " + sku));
        return convertToDtoWithTierPricing(product);
    }

    /**
     * Get product with tier-specific price.
     * Falls back to basePrice if no tier pricing exists.
     * @param sku Product SKU
     * @param tier Account tier
     * @return ProductDto with tier-specific price
     */
    public ProductDto getProductWithTierPrice(String sku, AccountTier tier) {
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with SKU: " + sku));
        
        ProductDto dto = convertToDto(product);
        
        // Try to find tier-specific pricing
        tierPricingRepository.findByProductSkuAndTier(sku, tier)
                .ifPresent(tierPricing -> {
                    dto.setTierPricing(List.of(convertToTierPricingDto(tierPricing)));
                });
        
        return dto;
    }

    /**
     * Get all root categories with children.
     * @return List of CategoryDto with hierarchy
     */
    public List<CategoryDto> getCategories() {
        List<Category> rootCategories = categoryRepository.findByParentIdIsNullOrderBySortOrderAsc();
        return rootCategories.stream()
                .map(this::convertToDtoWithChildren)
                .collect(Collectors.toList());
    }

    /**
     * Get category by ID with products.
     * @param id Category ID
     * @return CategoryDto
     */
    public CategoryDto getCategoryById(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));
        return convertToDto(category);
    }

    /**
     * Create new product.
     * @param dto Product data
     * @return Created ProductDto
     */
    @Transactional
    public ProductDto createProduct(ProductDto dto) {
        Category category = null;
        if (dto.getCategoryId() != null) {
            category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + dto.getCategoryId()));
        }
        
        Product product = Product.builder()
                .sku(dto.getSku())
                .name(dto.getName())
                .description(dto.getDescription())
                .basePrice(dto.getBasePrice())
                .inventoryLevel(dto.getInventoryLevel() != null ? dto.getInventoryLevel() : 0)
                .category(category)
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();
        
        Product savedProduct = productRepository.save(product);
        return convertToDto(savedProduct);
    }

    /**
     * Update inventory level.
     * Called by order service via events.
     * @param sku Product SKU
     * @param quantity New inventory quantity
     */
    @Transactional
    public void updateInventory(String sku, int quantity) {
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with SKU: " + sku));
        product.setInventoryLevel(quantity);
        productRepository.save(product);
    }

    /**
     * Convert Product entity to DTO.
     */
    private ProductDto convertToDto(Product product) {
        return ProductDto.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .description(product.getDescription())
                .basePrice(product.getBasePrice())
                .inventoryLevel(product.getInventoryLevel())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .isActive(product.getIsActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    /**
     * Convert Product entity to DTO with tier pricing.
     */
    private ProductDto convertToDtoWithTierPricing(Product product) {
        ProductDto dto = convertToDto(product);
        List<TierPricing> tierPricings = tierPricingRepository.findByProductId(product.getId());
        dto.setTierPricing(tierPricings.stream()
                .map(this::convertToTierPricingDto)
                .collect(Collectors.toList()));
        return dto;
    }

    /**
     * Convert Category entity to DTO.
     */
    private CategoryDto convertToDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .sortOrder(category.getSortOrder())
                .build();
    }

    /**
     * Convert Category entity to DTO with children.
     */
    private CategoryDto convertToDtoWithChildren(Category category) {
        CategoryDto dto = convertToDto(category);
        List<Category> children = categoryRepository.findByParentIdOrderBySortOrderAsc(category.getId());
        dto.setChildren(children.stream()
                .map(this::convertToDtoWithChildren)
                .collect(Collectors.toList()));
        return dto;
    }

    /**
     * Convert TierPricing entity to DTO.
     */
    private TierPricingDto convertToTierPricingDto(TierPricing tierPricing) {
        return TierPricingDto.builder()
                .id(tierPricing.getId())
                .productId(tierPricing.getProduct().getId())
                .tier(tierPricing.getTier())
                .price(tierPricing.getPrice())
                .build();
    }
}