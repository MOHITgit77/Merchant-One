#!/bin/bash
# ShopFlow End-to-End Verification Script
set -euo pipefail
BASE="http://localhost:8080"
PASS=0; FAIL=0; TOTAL=0
GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'; NC='\033[0m'

# macOS-compatible: extract body and status from curl output
do_curl() {
  local tmpfile=$(mktemp)
  local status=$(curl -s -o "$tmpfile" -w "%{http_code}" "$@")
  local body=$(cat "$tmpfile")
  rm -f "$tmpfile"
  echo "$body"
  echo "HTTP_STATUS:$status"
}

get_body() { echo "$1" | sed '/^HTTP_STATUS:/d'; }
get_status() { echo "$1" | grep 'HTTP_STATUS:' | cut -d: -f2; }

assert_status() {
  local desc="$1" expected="$2" actual="$3"
  TOTAL=$((TOTAL + 1))
  if [ "$expected" = "$actual" ]; then echo -e "  ${GREEN}✓${NC} $desc (HTTP $actual)"; PASS=$((PASS + 1))
  else echo -e "  ${RED}✗${NC} $desc (expected $expected, got $actual)"; FAIL=$((FAIL + 1)); fi
}

assert_contains() {
  local desc="$1" body="$2" expected="$3"
  TOTAL=$((TOTAL + 1))
  if echo "$body" | grep -q "$expected" 2>/dev/null; then echo -e "  ${GREEN}✓${NC} $desc"; PASS=$((PASS + 1))
  else echo -e "  ${RED}✗${NC} $desc (missing: $expected)"; FAIL=$((FAIL + 1)); fi
}

assert_not_contains() {
  local desc="$1" body="$2" unexpected="$3"
  TOTAL=$((TOTAL + 1))
  if echo "$body" | grep -q "$unexpected" 2>/dev/null; then echo -e "  ${RED}✗${NC} $desc (found: $unexpected)"; FAIL=$((FAIL + 1))
  else echo -e "  ${GREEN}✓${NC} $desc"; PASS=$((PASS + 1)); fi
}

assert_eq() {
  local desc="$1" expected="$2" actual="$3"
  TOTAL=$((TOTAL + 1))
  if [ "$expected" = "$actual" ]; then echo -e "  ${GREEN}✓${NC} $desc ($actual)"; PASS=$((PASS + 1))
  else echo -e "  ${RED}✗${NC} $desc (expected $expected, got $actual)"; FAIL=$((FAIL + 1)); fi
}

jval() { echo "$1" | python3 -c "import sys,json; print(json.load(sys.stdin)$2)" 2>/dev/null || echo "PARSE_ERR"; }

echo ""
echo "============================================="
echo "  ShopFlow E2E Verification"
echo "============================================="

# ==== 1. AUTH ====
echo -e "\n${YELLOW}1. AUTHENTICATION${NC}"

R=$(do_curl -X POST "$BASE/api/auth/register" -H "Content-Type: application/json" -d '{"name":"Merchant A","email":"a@test.com","password":"Pass1234!"}')
B=$(get_body "$R"); S=$(get_status "$R")
assert_status "Register Merchant A" "201" "$S"
assert_contains "Has access token" "$B" "accessToken"

R=$(do_curl -X POST "$BASE/api/auth/register" -H "Content-Type: application/json" -d '{"name":"Merchant B","email":"b@test.com","password":"Pass1234!"}')
S=$(get_status "$R")
assert_status "Register Merchant B" "201" "$S"

R=$(do_curl -X POST "$BASE/api/auth/login" -H "Content-Type: application/json" -d '{"email":"a@test.com","password":"Pass1234!"}')
B=$(get_body "$R"); S=$(get_status "$R")
assert_status "Login Merchant A" "200" "$S"
TOKEN_A=$(jval "$B" "['data']['accessToken']")

R=$(do_curl -X POST "$BASE/api/auth/login" -H "Content-Type: application/json" -d '{"email":"b@test.com","password":"Pass1234!"}')
B=$(get_body "$R")
TOKEN_B=$(jval "$B" "['data']['accessToken']")

R=$(do_curl -X POST "$BASE/api/auth/login" -H "Content-Type: application/json" -d '{"email":"a@test.com","password":"wrong"}')
S=$(get_status "$R")
assert_status "Invalid login rejected" "401" "$S"

R=$(do_curl -X POST "$BASE/api/auth/register" -H "Content-Type: application/json" -d '{"name":"Dup","email":"a@test.com","password":"Pass1234!"}')
S=$(get_status "$R")
assert_status "Duplicate email rejected" "409" "$S"

R=$(do_curl -X GET "$BASE/api/stores")
S=$(get_status "$R")
assert_status "Unauthenticated access rejected" "401" "$S"

AUTH_A="Authorization: Bearer $TOKEN_A"
AUTH_B="Authorization: Bearer $TOKEN_B"

# ==== 2. STORE ====
echo -e "\n${YELLOW}2. STORE SETUP${NC}"

R=$(do_curl -X POST "$BASE/api/stores" -H "$AUTH_A" -H "Content-Type: application/json" \
  -d '{"name":"Alpha Electronics","description":"Premium electronics","shopCategory":"ELECTRONICS","address":"123 Main St","phone":"+919876543210","email":"alpha@test.com"}')
B=$(get_body "$R"); S=$(get_status "$R")
assert_status "Create store A" "201" "$S"
assert_contains "Has slug" "$B" "slug"
STORE_A=$(jval "$B" "['data']['id']")
SLUG_A=$(jval "$B" "['data']['slug']")
echo "  Store A: $STORE_A (slug: $SLUG_A)"

R=$(do_curl -X POST "$BASE/api/stores" -H "$AUTH_B" -H "Content-Type: application/json" -d '{"name":"Beta Grocery","shopCategory":"GROCERY"}')
B=$(get_body "$R"); S=$(get_status "$R")
assert_status "Create store B" "201" "$S"
STORE_B=$(jval "$B" "['data']['id']")

R=$(do_curl -X PUT "$BASE/api/stores/$STORE_A" -H "$AUTH_A" -H "Content-Type: application/json" -d '{"name":"Alpha Electronics Pro"}')
S=$(get_status "$R")
assert_status "Update store" "200" "$S"

R=$(do_curl -X PATCH "$BASE/api/stores/$STORE_A/publish" -H "$AUTH_A")
S=$(get_status "$R")
assert_status "Publish store" "200" "$S"

R=$(do_curl -X GET "$BASE/api/stores/$STORE_A" -H "$AUTH_A")
B=$(get_body "$R"); S=$(get_status "$R")
assert_status "Get store" "200" "$S"
assert_contains "Store published" "$B" "true"

# ==== 3. SETTINGS ====
echo -e "\n${YELLOW}3. BUSINESS HOURS & SETTINGS${NC}"

R=$(do_curl -X PUT "$BASE/api/stores/$STORE_A/hours" -H "$AUTH_A" -H "Content-Type: application/json" \
  -d '[{"dayOfWeek":"MONDAY","openTime":"09:00","closeTime":"21:00","isOpen":true},{"dayOfWeek":"SUNDAY","isOpen":false}]')
S=$(get_status "$R")
assert_status "Set business hours" "200" "$S"

R=$(do_curl -X POST "$BASE/api/stores/$STORE_A/holidays" -H "$AUTH_A" -H "Content-Type: application/json" \
  -d '{"holidayDate":"2026-12-25","reason":"Christmas"}')
S=$(get_status "$R")
assert_status "Add holiday" "201" "$S"

R=$(do_curl -X PUT "$BASE/api/stores/$STORE_A/delivery" -H "$AUTH_A" -H "Content-Type: application/json" \
  -d '{"deliveryEnabled":true,"deliveryRadius":10.0,"minimumOrderAmount":500,"deliveryFee":50}')
S=$(get_status "$R")
assert_status "Update delivery" "200" "$S"

R=$(do_curl -X PUT "$BASE/api/stores/$STORE_A/payments" -H "$AUTH_A" -H "Content-Type: application/json" \
  -d '[{"paymentMethod":"CASH","enabled":true},{"paymentMethod":"UPI","enabled":true,"details":"upi@merchant"}]')
S=$(get_status "$R")
assert_status "Update payments" "200" "$S"

# ==== 4. CATEGORIES ====
echo -e "\n${YELLOW}4. CATEGORIES${NC}"

R=$(do_curl -X POST "$BASE/api/stores/$STORE_A/categories" -H "$AUTH_A" -H "Content-Type: application/json" -d '{"name":"Smartphones","description":"Mobile phones"}')
B=$(get_body "$R"); S=$(get_status "$R")
assert_status "Create category" "201" "$S"
CAT_PHONES=$(jval "$B" "['data']['id']")

R=$(do_curl -X POST "$BASE/api/stores/$STORE_A/categories" -H "$AUTH_A" -H "Content-Type: application/json" -d '{"name":"Accessories"}')
B=$(get_body "$R"); S=$(get_status "$R")
assert_status "Create category 2" "201" "$S"
CAT_ACC=$(jval "$B" "['data']['id']")

R=$(do_curl -X GET "$BASE/api/stores/$STORE_A/categories" -H "$AUTH_A")
B=$(get_body "$R"); S=$(get_status "$R")
assert_status "List categories" "200" "$S"
assert_contains "Has Smartphones" "$B" "Smartphones"

R=$(do_curl -X PUT "$BASE/api/stores/$STORE_A/categories/$CAT_ACC" -H "$AUTH_A" -H "Content-Type: application/json" -d '{"name":"Phone Accessories"}')
S=$(get_status "$R")
assert_status "Update category" "200" "$S"

# ==== 5. PRODUCTS ====
echo -e "\n${YELLOW}5. PRODUCTS & VARIANTS${NC}"

R=$(do_curl -X POST "$BASE/api/stores/$STORE_A/products" -H "$AUTH_A" -H "Content-Type: application/json" \
  -d "{\"name\":\"iPhone 15 Pro\",\"description\":\"Latest Apple\",\"price\":134900,\"costPrice\":125000,\"compareAtPrice\":139900,\"taxPercent\":18,\"categoryId\":\"$CAT_PHONES\",\"unit\":\"PCS\",\"trackInventory\":true,\"lowStockThreshold\":5}")
B=$(get_body "$R"); S=$(get_status "$R")
assert_status "Create product" "201" "$S"
assert_contains "Has SKU" "$B" "sku"
PRODUCT_A=$(jval "$B" "['data']['id']")
VARIANT_A=$(jval "$B" "['data']['variants'][0]['id']")
SKU_A=$(jval "$B" "['data']['variants'][0]['sku']")
echo "  Product: $PRODUCT_A, Variant: $VARIANT_A, SKU: $SKU_A"

R=$(do_curl -X POST "$BASE/api/stores/$STORE_A/products" -H "$AUTH_A" -H "Content-Type: application/json" \
  -d "{\"name\":\"USB-C Cable\",\"price\":499,\"costPrice\":200,\"taxPercent\":18,\"categoryId\":\"$CAT_ACC\",\"unit\":\"PCS\",\"trackInventory\":true,\"lowStockThreshold\":10}")
B=$(get_body "$R"); S=$(get_status "$R")
assert_status "Create product 2" "201" "$S"
PRODUCT_B=$(jval "$B" "['data']['id']")
VARIANT_B=$(jval "$B" "['data']['variants'][0]['id']")

R=$(do_curl -X GET "$BASE/api/stores/$STORE_A/products" -H "$AUTH_A")
S=$(get_status "$R")
assert_status "List products" "200" "$S"

R=$(do_curl -X GET "$BASE/api/stores/$STORE_A/products/$PRODUCT_A" -H "$AUTH_A")
S=$(get_status "$R")
assert_status "Get product detail" "200" "$S"

R=$(do_curl -X POST "$BASE/api/stores/$STORE_A/products/$PRODUCT_A/duplicate" -H "$AUTH_A")
S=$(get_status "$R")
assert_status "Duplicate product" "201" "$S"

# ==== 6. OPENING STOCK ====
echo -e "\n${YELLOW}6. INVENTORY — OPENING STOCK${NC}"

R=$(do_curl -X POST "$BASE/api/stores/$STORE_A/inventory/stock-in" -H "$AUTH_A" -H "Content-Type: application/json" \
  -d "{\"variantId\":\"$VARIANT_A\",\"quantity\":50,\"unitCost\":125000,\"notes\":\"Opening stock\"}")
B=$(get_body "$R"); S=$(get_status "$R")
assert_status "Stock in 50 iPhones" "200" "$S"
assert_contains "STOCK_IN type" "$B" "STOCK_IN"
assert_contains "Before=0" "$B" "\"quantityBefore\":0"
assert_contains "After=50" "$B" "\"quantityAfter\":50"

R=$(do_curl -X POST "$BASE/api/stores/$STORE_A/inventory/stock-in" -H "$AUTH_A" -H "Content-Type: application/json" \
  -d "{\"variantId\":\"$VARIANT_B\",\"quantity\":200,\"unitCost\":200,\"notes\":\"Opening stock\"}")
S=$(get_status "$R")
assert_status "Stock in 200 cables" "200" "$S"

# ==== 7. PURCHASES ====
echo -e "\n${YELLOW}7. PURCHASES & BATCHES${NC}"

R=$(do_curl -X POST "$BASE/api/stores/$STORE_A/purchases" -H "$AUTH_A" -H "Content-Type: application/json" \
  -d "{\"supplierName\":\"Apple Inc\",\"supplierContact\":\"supplier@apple.com\",\"items\":[{\"variantId\":\"$VARIANT_A\",\"quantity\":10,\"unitCost\":120000,\"batchNumber\":\"BATCH-2026-09\",\"expiryDate\":\"2027-12-31\"}]}")
B=$(get_body "$R"); S=$(get_status "$R")
assert_status "Create purchase" "201" "$S"
assert_contains "Has PO number" "$B" "PO-"
PURCHASE_ID=$(jval "$B" "['data']['id']")

# Verify stock: 50+10=60
R=$(do_curl -X GET "$BASE/api/stores/$STORE_A/products/$PRODUCT_A" -H "$AUTH_A")
B=$(get_body "$R")
STOCK=$(jval "$B" "['data']['variants'][0]['quantityOnHand']")
assert_eq "Purchase increased stock to 60" "60" "$STOCK"

R=$(do_curl -X GET "$BASE/api/stores/$STORE_A/purchases" -H "$AUTH_A"); assert_status "List purchases" "200" "$(get_status "$R")"
R=$(do_curl -X GET "$BASE/api/stores/$STORE_A/purchases/$PURCHASE_ID" -H "$AUTH_A"); assert_status "Get purchase detail" "200" "$(get_status "$R")"

R=$(do_curl -X GET "$BASE/api/stores/$STORE_A/batches" -H "$AUTH_A")
B=$(get_body "$R"); S=$(get_status "$R")
assert_status "List batches" "200" "$S"
assert_contains "Has batch number" "$B" "BATCH-2026-09"

R=$(do_curl -X GET "$BASE/api/stores/$STORE_A/batches/expiring" -H "$AUTH_A")
assert_status "Get expiring batches" "200" "$(get_status "$R")"

# ==== 8. ADJUSTMENTS ====
echo -e "\n${YELLOW}8. STOCK ADJUSTMENTS${NC}"

R=$(do_curl -X POST "$BASE/api/stores/$STORE_A/inventory/adjust" -H "$AUTH_A" -H "Content-Type: application/json" \
  -d "{\"variantId\":\"$VARIANT_A\",\"newQuantity\":55,\"reason\":\"Damaged 5 units\"}")
B=$(get_body "$R"); S=$(get_status "$R")
assert_status "Adjust stock" "200" "$S"
assert_contains "ADJUSTMENT type" "$B" "ADJUSTMENT"
assert_contains "Before=60" "$B" "\"quantityBefore\":60"
assert_contains "After=55" "$B" "\"quantityAfter\":55"

R=$(do_curl -X POST "$BASE/api/stores/$STORE_A/inventory/stock-out" -H "$AUTH_A" -H "Content-Type: application/json" \
  -d "{\"variantId\":\"$VARIANT_A\",\"quantity\":5,\"notes\":\"Display units\"}")
B=$(get_body "$R"); S=$(get_status "$R")
assert_status "Stock out 5" "200" "$S"
assert_contains "After=50" "$B" "\"quantityAfter\":50"

# ==== 9. SALES ====
echo -e "\n${YELLOW}9. SALES / BILLING${NC}"

IDEMP="sale-e2e-$(date +%s)"
R=$(do_curl -X POST "$BASE/api/stores/$STORE_A/sales" -H "$AUTH_A" -H "Content-Type: application/json" -H "Idempotency-Key: $IDEMP" \
  -d "{\"items\":[{\"variantId\":\"$VARIANT_A\",\"quantity\":2},{\"variantId\":\"$VARIANT_B\",\"quantity\":3}],\"paymentMethod\":\"CASH\",\"customerName\":\"John Doe\",\"customerPhone\":\"+919999999999\",\"discountAmount\":100}")
B=$(get_body "$R"); S=$(get_status "$R")
assert_status "Create sale" "201" "$S"
assert_contains "Has invoice" "$B" "INV-"
assert_contains "Has customer" "$B" "John Doe"
SALE_ID=$(jval "$B" "['data']['id']")

# Verify: iPhones 50-2=48
R=$(do_curl -X GET "$BASE/api/stores/$STORE_A/products/$PRODUCT_A" -H "$AUTH_A")
STOCK=$(jval "$(get_body "$R")" "['data']['variants'][0]['quantityOnHand']")
assert_eq "iPhone stock deducted to 48" "48" "$STOCK"

# Verify: cables 200-3=197
R=$(do_curl -X GET "$BASE/api/stores/$STORE_A/products/$PRODUCT_B" -H "$AUTH_A")
STOCK=$(jval "$(get_body "$R")" "['data']['variants'][0]['quantityOnHand']")
assert_eq "Cable stock deducted to 197" "197" "$STOCK"

# ==== 10. IDEMPOTENCY ====
echo -e "\n${YELLOW}10. IDEMPOTENCY${NC}"

R=$(do_curl -X POST "$BASE/api/stores/$STORE_A/sales" -H "$AUTH_A" -H "Content-Type: application/json" -H "Idempotency-Key: $IDEMP" \
  -d "{\"items\":[{\"variantId\":\"$VARIANT_A\",\"quantity\":2}],\"paymentMethod\":\"CASH\"}")
B=$(get_body "$R"); S=$(get_status "$R")
DUP_ID=$(jval "$B" "['data']['id']")
assert_eq "Idempotent returns same sale" "$SALE_ID" "$DUP_ID"

R=$(do_curl -X GET "$BASE/api/stores/$STORE_A/products/$PRODUCT_A" -H "$AUTH_A")
STOCK=$(jval "$(get_body "$R")" "['data']['variants'][0]['quantityOnHand']")
assert_eq "Idempotent: stock unchanged (48)" "48" "$STOCK"

# ==== 11. INSUFFICIENT STOCK ====
echo -e "\n${YELLOW}11. INSUFFICIENT STOCK${NC}"

R=$(do_curl -X POST "$BASE/api/stores/$STORE_A/sales" -H "$AUTH_A" -H "Content-Type: application/json" \
  -d "{\"items\":[{\"variantId\":\"$VARIANT_A\",\"quantity\":999}],\"paymentMethod\":\"CASH\"}")
B=$(get_body "$R"); S=$(get_status "$R")
assert_status "Insufficient stock rejected" "422" "$S"
assert_contains "Error mentions stock" "$B" "stock"

R=$(do_curl -X GET "$BASE/api/stores/$STORE_A/products/$PRODUCT_A" -H "$AUTH_A")
STOCK=$(jval "$(get_body "$R")" "['data']['variants'][0]['quantityOnHand']")
assert_eq "Stock unchanged after reject (48)" "48" "$STOCK"

# ==== 12. LEDGER ====
echo -e "\n${YELLOW}12. INVENTORY LEDGER${NC}"

R=$(do_curl -X GET "$BASE/api/stores/$STORE_A/inventory/history?size=50" -H "$AUTH_A")
B=$(get_body "$R"); S=$(get_status "$R")
assert_status "Get ledger history" "200" "$S"
assert_contains "Has STOCK_IN" "$B" "STOCK_IN"
assert_contains "Has SALE" "$B" "SALE"
assert_contains "Has ADJUSTMENT" "$B" "ADJUSTMENT"
assert_contains "Has STOCK_OUT" "$B" "STOCK_OUT"

R=$(do_curl -X GET "$BASE/api/stores/$STORE_A/inventory/history?type=SALE" -H "$AUTH_A")
B=$(get_body "$R")
assert_contains "Filtered has SALE" "$B" "SALE"

# ==== 13. SALES HISTORY ====
echo -e "\n${YELLOW}13. SALES HISTORY${NC}"

R=$(do_curl -X GET "$BASE/api/stores/$STORE_A/sales" -H "$AUTH_A")
assert_status "List sales" "200" "$(get_status "$R")"

R=$(do_curl -X GET "$BASE/api/stores/$STORE_A/sales/$SALE_ID" -H "$AUTH_A")
B=$(get_body "$R"); S=$(get_status "$R")
assert_status "Get sale detail" "200" "$S"
assert_contains "Has items" "$B" "items"
assert_contains "Has taxAmount" "$B" "taxAmount"

# ==== 14. STOREFRONT ====
echo -e "\n${YELLOW}14. PUBLIC STOREFRONT${NC}"

R=$(do_curl -X GET "$BASE/api/storefront/$SLUG_A")
B=$(get_body "$R"); S=$(get_status "$R")
assert_status "Public store" "200" "$S"
assert_contains "Has store name" "$B" "Alpha Electronics"

R=$(do_curl -X GET "$BASE/api/storefront/$SLUG_A/categories")
B=$(get_body "$R")
assert_status "Public categories" "200" "$(get_status "$R")"
assert_contains "Has Smartphones" "$B" "Smartphones"

R=$(do_curl -X GET "$BASE/api/storefront/$SLUG_A/products")
assert_status "Public products" "200" "$(get_status "$R")"

R=$(do_curl -X GET "$BASE/api/storefront/nonexistent-slug")
assert_status "Invalid slug 404" "404" "$(get_status "$R")"

# ==== 15. DASHBOARD ====
echo -e "\n${YELLOW}15. DASHBOARD METRICS${NC}"

R=$(do_curl -X GET "$BASE/api/stores/$STORE_A/dashboard/metrics" -H "$AUTH_A")
B=$(get_body "$R"); S=$(get_status "$R")
assert_status "Dashboard metrics" "200" "$S"
assert_contains "Has todaySales" "$B" "todaySales"
assert_contains "Has todayRevenue" "$B" "todayRevenue"
assert_contains "Has totalProducts" "$B" "totalProducts"
assert_contains "Has lowStockCount" "$B" "lowStockCount"

# ==== 16. SECURITY ====
echo -e "\n${YELLOW}16. CROSS-TENANT SECURITY${NC}"

R=$(do_curl -X GET "$BASE/api/stores/$STORE_A" -H "$AUTH_B"); assert_status "X-tenant: store GET" "404" "$(get_status "$R")"
R=$(do_curl -X GET "$BASE/api/stores/$STORE_A/categories" -H "$AUTH_B"); assert_status "X-tenant: categories" "403" "$(get_status "$R")"
R=$(do_curl -X GET "$BASE/api/stores/$STORE_A/products" -H "$AUTH_B"); assert_status "X-tenant: products" "403" "$(get_status "$R")"
R=$(do_curl -X GET "$BASE/api/stores/$STORE_A/inventory/history" -H "$AUTH_B"); assert_status "X-tenant: inventory" "403" "$(get_status "$R")"
R=$(do_curl -X GET "$BASE/api/stores/$STORE_A/sales" -H "$AUTH_B"); assert_status "X-tenant: sales" "403" "$(get_status "$R")"
R=$(do_curl -X GET "$BASE/api/stores/$STORE_A/purchases" -H "$AUTH_B"); assert_status "X-tenant: purchases" "403" "$(get_status "$R")"
R=$(do_curl -X GET "$BASE/api/stores/$STORE_A/dashboard/metrics" -H "$AUTH_B"); assert_status "X-tenant: dashboard" "403" "$(get_status "$R")"
R=$(do_curl -X POST "$BASE/api/stores/$STORE_A/products" -H "$AUTH_B" -H "Content-Type: application/json" -d '{"name":"Hack","price":1}'); assert_status "X-tenant: create product" "403" "$(get_status "$R")"
R=$(do_curl -X POST "$BASE/api/stores/$STORE_A/sales" -H "$AUTH_B" -H "Content-Type: application/json" -H "Idempotency-Key: x-test" -d "{\"items\":[{\"variantId\":\"$VARIANT_A\",\"quantity\":1}],\"paymentMethod\":\"CASH\"}"); assert_status "X-tenant: create sale" "403" "$(get_status "$R")"
R=$(do_curl -X POST "$BASE/api/stores/$STORE_A/inventory/stock-in" -H "$AUTH_B" -H "Content-Type: application/json" -d "{\"variantId\":\"$VARIANT_A\",\"quantity\":1000}"); assert_status "X-tenant: stock-in" "403" "$(get_status "$R")"

# ==== 17. VALIDATION ====
echo -e "\n${YELLOW}17. VALIDATION${NC}"

R=$(do_curl -X POST "$BASE/api/stores/$STORE_A/sales" -H "$AUTH_A" -H "Content-Type: application/json" -d '{"items":[]}'); assert_status "Empty sale rejected" "400" "$(get_status "$R")"
R=$(do_curl -X POST "$BASE/api/stores/$STORE_A/products" -H "$AUTH_A" -H "Content-Type: application/json" -d '{"price":100}'); assert_status "No-name product rejected" "400" "$(get_status "$R")"
R=$(do_curl -X POST "$BASE/api/stores/$STORE_A/products" -H "$AUTH_A" -H "Content-Type: application/json" -d '{"name":"Test","price":-100}'); assert_status "Negative price rejected" "400" "$(get_status "$R")"
R=$(do_curl -X POST "$BASE/api/stores/$STORE_A/categories" -H "$AUTH_A" -H "Content-Type: application/json" -d '{}'); assert_status "No-name category rejected" "400" "$(get_status "$R")"
R=$(do_curl -X POST "$BASE/api/stores/$STORE_A/inventory/stock-out" -H "$AUTH_A" -H "Content-Type: application/json" -d "{\"variantId\":\"$VARIANT_A\",\"quantity\":9999}"); assert_status "Over-stock-out rejected" "422" "$(get_status "$R")"

# ==== 18. LIFECYCLE ====
echo -e "\n${YELLOW}18. PRODUCT & STORE LIFECYCLE${NC}"

R=$(do_curl -X PATCH "$BASE/api/stores/$STORE_A/products/$PRODUCT_A/deactivate" -H "$AUTH_A"); assert_status "Deactivate product" "200" "$(get_status "$R")"
R=$(do_curl -X PATCH "$BASE/api/stores/$STORE_A/products/$PRODUCT_A/activate" -H "$AUTH_A"); assert_status "Activate product" "200" "$(get_status "$R")"
R=$(do_curl -X PATCH "$BASE/api/stores/$STORE_A/unpublish" -H "$AUTH_A"); assert_status "Unpublish store" "200" "$(get_status "$R")"
R=$(do_curl -X GET "$BASE/api/storefront/$SLUG_A"); assert_status "Unpublished not on storefront" "404" "$(get_status "$R")"
R=$(do_curl -X PATCH "$BASE/api/stores/$STORE_A/publish" -H "$AUTH_A"); assert_status "Republish store" "200" "$(get_status "$R")"

# ==== 19. OPENAPI ====
echo -e "\n${YELLOW}19. OPENAPI${NC}"
R=$(do_curl -X GET "$BASE/api-docs"); assert_status "OpenAPI docs" "200" "$(get_status "$R")"

# ==== 20. ORDER MANAGEMENT ====
echo -e "\n${YELLOW}20. ORDER MANAGEMENT${NC}"

R=$(do_curl -X POST "$BASE/api/storefront/$SLUG_A/orders" -H "Content-Type: application/json" \
  -d "{\"items\":[{\"variantId\":\"$VARIANT_A\",\"quantity\":1}],\"customerName\":\"Jane Smith\",\"customerPhone\":\"1234567890\",\"orderType\":\"PICKUP\",\"paymentMethod\":\"CASH\"}")
B=$(get_body "$R"); S=$(get_status "$R")
assert_status "Customer creates order" "201" "$S"
assert_contains "Has order number" "$B" "ORD-"
ORDER_ID=$(jval "$B" "['data']['id']")

R=$(do_curl -X GET "$BASE/api/stores/$STORE_A/orders" -H "$AUTH_A")
B=$(get_body "$R"); S=$(get_status "$R")
assert_status "Merchant lists orders" "200" "$S"
assert_contains "Order is PENDING" "$B" "PENDING"

R=$(do_curl -X PATCH "$BASE/api/stores/$STORE_A/orders/$ORDER_ID/accept" -H "$AUTH_A")
S=$(get_status "$R")
assert_status "Merchant accepts order" "200" "$S"

R=$(do_curl -X PATCH "$BASE/api/stores/$STORE_A/orders/$ORDER_ID/status" -H "$AUTH_A" -H "Content-Type: application/json" -d '{"status":"READY"}')
S=$(get_status "$R")
assert_status "Merchant marks order READY" "200" "$S"

R=$(do_curl -X PATCH "$BASE/api/stores/$STORE_A/orders/$ORDER_ID/complete" -H "$AUTH_A")
S=$(get_status "$R")
assert_status "Merchant completes order" "200" "$S"

# Verify stock deducted
R=$(do_curl -X GET "$BASE/api/stores/$STORE_A/products/$PRODUCT_A" -H "$AUTH_A")
STOCK=$(jval "$(get_body "$R")" "['data']['variants'][0]['quantityOnHand']")
assert_eq "iPhone stock deducted to 47 (48 - 1 order)" "47" "$STOCK"

# ==== RESULTS ====
echo ""
echo "============================================="
echo "  RESULTS"
echo "============================================="
echo ""
echo -e "  Total:  $TOTAL"
echo -e "  ${GREEN}Passed: $PASS${NC}"
echo -e "  ${RED}Failed: $FAIL${NC}"
echo ""
if [ "$FAIL" -eq 0 ]; then echo -e "  ${GREEN}ALL TESTS PASSED ✓${NC}"
else echo -e "  ${RED}$FAIL TESTS FAILED ✗${NC}"; fi
echo ""
exit $FAIL
