# Storefront API Documentation

This document outlines the public REST API endpoints available for the Storefront. These APIs can be consumed by customer-facing applications (e.g., mobile apps, web storefronts, or POS kiosks) to browse products and place orders.

**Base URL Pattern:** `/api/storefront/{slug}`

Where `{slug}` is the unique, URL-friendly identifier for the merchant's store. 
All Storefront endpoints are completely public and do not require authentication tokens.

---

## 1. Store Information

Retrieve public details about a specific store, such as name, contact info, and supported fulfillment/payment methods.

- **Method:** `GET`
- **URL:** `/api/storefront/{slug}`
- **Purpose:** Fetch store information and settings required for the checkout process (e.g., verifying if PICKUP/DELIVERY are enabled).

**Example Request:**
```http
GET /api/storefront/my-shop
```

**Example Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "name": "My Shop",
    "slug": "my-shop",
    "description": "The best shop in town",
    "logoUrl": "https://example.com/logo.png",
    "contact": {
      "phone": "+919876543210",
      "email": "contact@myshop.com",
      "address": "123 Main St"
    },
    "fulfillment": {
      "pickupEnabled": true,
      "deliveryEnabled": true
    },
    "paymentMethods": [
      "CASH",
      "UPI",
      "CREDIT_CARD"
    ]
  }
}
```

**Error Responses:**
- `404 Not Found`: If the store does not exist or is not published.

---

## 2. Categories

Retrieve all active product categories for a store.

- **Method:** `GET`
- **URL:** `/api/storefront/{slug}/categories`
- **Purpose:** Used for catalog navigation and filtering.

**Example Request:**
```http
GET /api/storefront/my-shop/categories
```

**Example Response (200 OK):**
```json
{
  "success": true,
  "data": [
    {
      "id": "cat-uuid-1",
      "name": "Electronics",
      "description": "Gadgets and devices",
      "sortOrder": 1
    },
    {
      "id": "cat-uuid-2",
      "name": "Accessories",
      "description": "Cables and cases",
      "sortOrder": 2
    }
  ]
}
```

---

## 3. Products

Retrieve active products available in the store. Includes pricing and variant information, but hides sensitive merchant data like cost price and reserved quantities.

- **Method:** `GET`
- **URL:** `/api/storefront/{slug}/products`
- **Purpose:** Render the product listing page.
- **Query Parameters:**
  - `categoryId` (Optional) - Filter by category UUID.
  - `search` (Optional) - Filter by product name/description.
  - `page` (Optional) - Pagination (default: 0).
  - `size` (Optional) - Page size (default: 20).

**Example Request:**
```http
GET /api/storefront/my-shop/products?categoryId=cat-uuid-1&page=0
```

**Example Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "prod-uuid",
        "name": "Smartphone X",
        "description": "Latest model",
        "categoryId": "cat-uuid-1",
        "variants": [
          {
            "id": "var-uuid",
            "name": "Default",
            "sku": "PHONE-X",
            "price": 50000.00,
            "compareAtPrice": 55000.00,
            "quantityOnHand": 15,
            "imageUrl": "https://example.com/img.jpg"
          }
        ]
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

---

## 4. Product Details

Retrieve detailed information for a single product.

- **Method:** `GET`
- **URL:** `/api/storefront/{slug}/products/{productId}`
- **Purpose:** Render the single product detail page.

**Example Request:**
```http
GET /api/storefront/my-shop/products/prod-uuid
```

**Example Response (200 OK):**
(Same JSON structure as a single item in the Products array above).

**Error Responses:**
- `404 Not Found`: If the product does not exist, belongs to another store, or is inactive.

---

## 5. Order Creation

Submit a new customer order. Stock is validated during creation but is **not reserved** until the merchant explicitly accepts the order.

- **Method:** `POST`
- **URL:** `/api/storefront/{slug}/orders`
- **Purpose:** Place an order on behalf of a customer.

**Validation Rules:**
- `orderType` must be `PICKUP` or `DELIVERY`. Store must have the respective fulfillment setting enabled.
- `paymentMethod` must be in the store's supported payment methods list.
- Order must contain at least 1 item.
- `quantity` for each item must be greater than 0.

**Example Request:**
```http
POST /api/storefront/my-shop/orders
Content-Type: application/json

{
  "customerName": "Jane Doe",
  "customerPhone": "+919876543210",
  "orderType": "PICKUP",
  "paymentMethod": "CASH",
  "items": [
    {
      "variantId": "var-uuid-1",
      "quantity": 2
    },
    {
      "variantId": "var-uuid-2",
      "quantity": 1
    }
  ]
}
```

**Example Response (201 Created):**
```json
{
  "success": true,
  "message": "Order placed successfully",
  "data": {
    "id": "order-uuid",
    "orderNumber": "ORD-000001",
    "status": "PENDING",
    "orderType": "PICKUP",
    "paymentMethod": "CASH",
    "customerName": "Jane Doe",
    "customerPhone": "+919876543210",
    "subtotal": 100500.00,
    "taxAmount": 18090.00,
    "totalAmount": 118590.00,
    "createdAt": "2026-09-17T13:45:00Z",
    "items": [
      {
        "id": "item-uuid-1",
        "variantId": "var-uuid-1",
        "productName": "Smartphone X",
        "variantName": "Default",
        "sku": "PHONE-X",
        "quantity": 2,
        "unitPrice": 50000.00,
        "taxAmount": 18000.00,
        "lineTotal": 118000.00
      }
    ]
  }
}
```

**Error Responses:**
- `422 Unprocessable Entity`: "INSUFFICIENT_STOCK" if any variant does not have enough available stock.
- `422 Unprocessable Entity`: "DELIVERY_ONLY" or "PICKUP_ONLY" if the requested `orderType` is not supported by the store.
- `400 Bad Request`: Validation errors on the request payload.
