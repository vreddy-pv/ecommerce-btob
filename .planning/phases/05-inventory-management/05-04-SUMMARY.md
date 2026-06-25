# Plan 05-04 SUMMARY: Frontend — Inventory, Low-Stock & Cancel UI

## Commits
1. `7b185e4` — Update models (OrderStatus CANCELLED, ProductDto reservedInventory/reorderPoint) and services (getLowStockProducts, adjustInventory, cancelOrder)
2. `a55c3f5` — Fix product card stock display: show "Out of stock" (red), "Low stock" (orange), "In stock" (green) based on available=inventoryLevel-reservedInventory vs reorderPoint
3. `2d6bf7f` — Add cancel order button on order detail page for PENDING/CONFIRMED orders with confirmation
4. `9747166` — Add stock validation before order submission (checks available stock via catalog-service)
5. `6d238b1` — Add low-stock alert widget to catalog page with link to inventory management
6. `f679cb5` — Add inventory route and nav link in sidebar
7. `4610d36` — Create admin inventory management page with delta-based adjustment

## Verification
- [x] OrderStatus type includes CANCELLED
- [x] ProductDto includes reservedInventory and reorderPoint
- [x] CatalogService.getLowStockProducts, adjustInventory, getProductStock methods added
- [x] OrderService.cancelOrder method added
- [x] Product card shows proper stock status (out of stock / low stock / in stock)
- [x] Cancel button appears on order detail for cancellable orders
- [x] Stock validation runs before order submission
- [x] Low-stock widget visible on catalog page
- [x] Inventory admin page at /inventory route
- [x] Inventory link in sidebar navigation

## Files Modified/Created
- frontend/src/app/core/models/api.models.ts (modified)
- frontend/src/app/core/services/catalog.service.ts (modified)
- frontend/src/app/core/services/order.service.ts (modified)
- frontend/src/app/features/catalog/product-card.component.ts (modified)
- frontend/src/app/features/orders/order-detail.component.ts (modified)
- frontend/src/app/features/orders/cart-sidebar.component.ts (modified)
- frontend/src/app/features/catalog/catalog.component.ts (modified)
- frontend/src/app/app.routes.ts (modified)
- frontend/src/app/app.component.ts (modified)
- frontend/src/app/features/inventory/inventory-list.component.ts (created)
