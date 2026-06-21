package com.btob.catalog.service;

import com.btob.catalog.entity.AccountTier;
import com.btob.catalog.entity.Category;
import com.btob.catalog.entity.Product;
import com.btob.catalog.entity.TierPricing;
import com.btob.catalog.repository.CategoryRepository;
import com.btob.catalog.repository.ProductRepository;
import com.btob.catalog.repository.TierPricingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Initializes seed data for the catalog service.
 * Runs after Hibernate creates the schema.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final TierPricingRepository tierPricingRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (productRepository.count() > 0) {
            log.info("Database already seeded, skipping initialization");
            return;
        }

        log.info("Seeding catalog database with sample data...");

        // Create categories
        Category brakes = createCategory("Brakes", null, 1);
        Category engine = createCategory("Engine", null, 2);
        Category electrical = createCategory("Electrical", null, 3);
        Category suspension = createCategory("Suspension", null, 4);
        Category filters = createCategory("Filters", null, 5);

        // Create products
        Product brk001 = createProduct("BRK-001", "Front Brake Pads", "Ceramic front brake pads for passenger vehicles", 45.99, 150, brakes);
        Product brk002 = createProduct("BRK-002", "Rear Brake Pads", "Ceramic rear brake pads for passenger vehicles", 39.99, 120, brakes);
        Product brk003 = createProduct("BRK-003", "Brake Rotors", "Front brake rotors, vented", 65.50, 80, brakes);
        Product brk004 = createProduct("BRK-004", "Brake Caliper", "Front brake caliper assembly", 125.00, 45, brakes);
        Product brk005 = createProduct("BRK-005", "Brake Fluid", "DOT 4 brake fluid, 32oz", 12.99, 200, brakes);

        Product eng001 = createProduct("ENG-001", "Spark Plugs", "Iridium spark plugs, set of 4", 24.99, 300, engine);
        Product eng002 = createProduct("ENG-002", "Engine Oil", "Synthetic 5W-30 engine oil, 5qt", 29.99, 250, engine);
        Product eng003 = createProduct("ENG-003", "Oil Filter", "Premium oil filter", 8.99, 500, engine);
        Product eng004 = createProduct("ENG-004", "Air Filter", "Engine air filter", 15.99, 400, engine);
        Product eng005 = createProduct("ENG-005", "Timing Belt", "Timing belt kit with tensioner", 89.99, 60, engine);

        Product elc001 = createProduct("ELC-001", "Car Battery", "12V car battery, 600 CCA", 129.99, 75, electrical);
        Product elc002 = createProduct("ELC-002", "Alternator", "Remanufactured alternator", 149.99, 40, electrical);
        Product elc003 = createProduct("ELC-003", "Starter Motor", "Remanufactured starter motor", 99.99, 55, electrical);
        Product elc004 = createProduct("ELC-004", "Headlights", "LED headlight bulbs, pair", 34.99, 180, electrical);
        Product elc005 = createProduct("ELC-005", "Fuses", "Assorted automotive fuses, 50-pack", 9.99, 600, electrical);

        Product sus001 = createProduct("SUS-001", "Shock Absorbers", "Front shock absorbers, pair", 79.99, 90, suspension);
        Product sus002 = createProduct("SUS-002", "Struts", "Front strut assembly", 119.99, 65, suspension);
        Product sus003 = createProduct("SUS-003", "Control Arms", "Lower control arm with ball joint", 89.99, 50, suspension);

        Product flt001 = createProduct("FLT-001", "Cabin Air Filter", "Cabin air filter with activated carbon", 19.99, 350, filters);
        Product flt002 = createProduct("FLT-002", "Fuel Filter", "Inline fuel filter", 14.99, 280, filters);

        // Create tier pricing (SILVER 10%, GOLD 15%, PLATINUM 20% discount)
        createTierPricing(brk001, AccountTier.SILVER, 41.39);
        createTierPricing(brk001, AccountTier.GOLD, 39.09);
        createTierPricing(brk001, AccountTier.PLATINUM, 36.79);

        createTierPricing(brk002, AccountTier.SILVER, 35.99);
        createTierPricing(brk002, AccountTier.GOLD, 33.99);
        createTierPricing(brk002, AccountTier.PLATINUM, 31.99);

        createTierPricing(brk003, AccountTier.SILVER, 58.95);
        createTierPricing(brk003, AccountTier.GOLD, 55.68);
        createTierPricing(brk003, AccountTier.PLATINUM, 52.40);

        createTierPricing(eng001, AccountTier.SILVER, 22.49);
        createTierPricing(eng001, AccountTier.GOLD, 21.24);
        createTierPricing(eng001, AccountTier.PLATINUM, 19.99);

        createTierPricing(eng002, AccountTier.SILVER, 26.99);
        createTierPricing(eng002, AccountTier.GOLD, 25.49);
        createTierPricing(eng002, AccountTier.PLATINUM, 23.99);

        createTierPricing(elc001, AccountTier.SILVER, 116.99);
        createTierPricing(elc001, AccountTier.GOLD, 110.49);
        createTierPricing(elc001, AccountTier.PLATINUM, 103.99);

        createTierPricing(elc002, AccountTier.SILVER, 134.99);
        createTierPricing(elc002, AccountTier.GOLD, 127.49);
        createTierPricing(elc002, AccountTier.PLATINUM, 119.99);

        createTierPricing(sus001, AccountTier.SILVER, 71.99);
        createTierPricing(sus001, AccountTier.GOLD, 67.99);
        createTierPricing(sus001, AccountTier.PLATINUM, 63.99);

        createTierPricing(flt001, AccountTier.SILVER, 17.99);
        createTierPricing(flt001, AccountTier.GOLD, 16.99);
        createTierPricing(flt001, AccountTier.PLATINUM, 15.99);

        log.info("Catalog database seeded successfully with 5 categories, 20 products, and tier pricing");
    }

    private Category createCategory(String name, Category parent, int sortOrder) {
        Category category = Category.builder()
                .name(name)
                .parent(parent)
                .sortOrder(sortOrder)
                .build();
        return categoryRepository.save(category);
    }

    private Product createProduct(String sku, String name, String description, double basePrice, int inventoryLevel, Category category) {
        Product product = Product.builder()
                .sku(sku)
                .name(name)
                .description(description)
                .basePrice(BigDecimal.valueOf(basePrice))
                .inventoryLevel(inventoryLevel)
                .category(category)
                .isActive(true)
                .build();
        return productRepository.save(product);
    }

    private TierPricing createTierPricing(Product product, AccountTier tier, double price) {
        TierPricing tierPricing = TierPricing.builder()
                .product(product)
                .tier(tier)
                .price(BigDecimal.valueOf(price))
                .build();
        return tierPricingRepository.save(tierPricing);
    }
}