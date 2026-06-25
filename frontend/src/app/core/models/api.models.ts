// TypeScript interfaces matching backend DTOs
// Source: backend DTOs verified via codebase

export type AccountTier = 'STANDARD' | 'SILVER' | 'GOLD' | 'PLATINUM';
export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';

// AuthResponse from POST /api/auth/login
export interface AuthResponse {
  token: string;
  accountId: string;      // UUID as string
  email: string;
  tier: AccountTier;
  expiresIn: number;      // seconds (86400 = 24h)
}

// LoginRequest for POST /api/auth/login
export interface LoginRequest {
  email: string;
  password: string;
}

// ProductDto from GET /api/catalog/products
export interface ProductDto {
  id: string;
  sku: string;
  name: string;
  description: string;
  basePrice: number;
  inventoryLevel: number;
  reservedInventory: number;
  reorderPoint: number;
  categoryId: string;
  categoryName: string;
  isActive: boolean;
  tierPricing: TierPricingDto[];
  createdAt: string;      // ISO datetime
  updatedAt: string;
}

export interface TierPricingDto {
  id: string;
  productId: string;
  tier: AccountTier;
  price: number;
}

export interface CategoryDto {
  id: string;
  name: string;
  parentId: string | null;
  sortOrder: number;
  children: CategoryDto[];
}

// Spring Data Page<T> JSON shape
export interface Page<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: { sorted: boolean; unsorted: boolean; empty: boolean };
    offset: number;
    paged: boolean;
    unpaged: boolean;
  };
  totalElements: number;
  totalPages: number;
  number: number;         // current page
  size: number;           // page size
  first: boolean;
  last: boolean;
  empty: boolean;
}

// OrderResponse from GET /api/orders, GET /api/orders/{id}
export interface OrderResponse {
  id: string;
  accountId: string;
  status: OrderStatus;
  totalAmount: number;
  creditUsed: number;
  items: OrderItemDto[];
  createdAt: string;
  updatedAt: string;
}

export interface OrderItemDto {
  id: string;
  productSku: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
}

// CreateOrderRequest for POST /api/orders
export interface CreateOrderRequest {
  accountId: string;
  items: { productSku: string; quantity: number }[];
}

// Backend error response format (from GlobalExceptionHandler.java)
export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message?: string;       // present on 404/500
  errors?: Record<string, string>;  // present on 400 validation: { fieldName: "message" }
}
