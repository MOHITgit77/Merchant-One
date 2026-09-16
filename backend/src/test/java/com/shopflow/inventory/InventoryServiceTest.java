package com.shopflow.inventory;

import com.shopflow.exception.BusinessRuleException;
import com.shopflow.inventory.dto.LedgerEntryDto;
import com.shopflow.inventory.dto.StockAdjustmentRequest;
import com.shopflow.inventory.dto.StockMovementRequest;
import com.shopflow.product.Product;
import com.shopflow.product.ProductRepository;
import com.shopflow.product.ProductUnit;
import com.shopflow.product.ProductVariant;
import com.shopflow.product.ProductVariantRepository;
import com.shopflow.store.Store;
import com.shopflow.store.StoreRepository;
import com.shopflow.store.ShopCategory;
import com.shopflow.user.User;
import com.shopflow.user.UserRepository;
import com.shopflow.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class InventoryServiceTest {

    @Autowired private InventoryService inventoryService;
    @Autowired private StoreRepository storeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository variantRepository;
    @Autowired private InventoryLedgerRepository ledgerRepository;

    private User merchant;
    private Store store;
    private Product product;
    private ProductVariant variant;

    @BeforeEach
    public void setup() {
        ledgerRepository.deleteAll();
        variantRepository.deleteAll();
        productRepository.deleteAll();
        storeRepository.deleteAll();
        userRepository.deleteAll();

        merchant = new User();
        merchant.setName("Inventory Merchant");
        merchant.setEmail("inv" + System.currentTimeMillis() + "@merchant.com");
        merchant.setPasswordHash("hash");
        merchant.setRole(UserRole.MERCHANT);
        merchant = userRepository.save(merchant);

        store = new Store();
        store.setMerchantId(merchant.getId());
        store.setName("Inventory Store");
        store.setShopCategory(ShopCategory.GROCERY);
        store.setSlug("inv-store-" + System.currentTimeMillis());
        store = storeRepository.save(store);

        product = new Product();
        product.setStoreId(store.getId());
        product.setName("Inventory Product");
        product.setPrice(BigDecimal.TEN);
        product.setUnit(ProductUnit.PCS);
        product.setTrackInventory(true);
        product = productRepository.save(product);

        variant = new ProductVariant();
        variant.setProduct(product);
        variant.setName("Default");
        variant.setSku("INV-SKU-" + System.currentTimeMillis());
        variant.setPrice(BigDecimal.TEN);
        variant.setQuantityOnHand(10);
        variant = variantRepository.save(variant);
    }

    @Test
    public void testStockIn_ShouldIncreaseQuantityAndCreateLedger() {
        StockMovementRequest request = new StockMovementRequest();
        request.setVariantId(variant.getId());
        request.setQuantity(5);
        request.setNotes("Supplier delivery");

        LedgerEntryDto ledger = inventoryService.stockIn(store.getId(), request, merchant.getId());

        ProductVariant updated = variantRepository.findById(variant.getId()).orElseThrow();
        assertEquals(15, updated.getQuantityOnHand());
        assertEquals(MovementType.STOCK_IN, ledger.getMovementType());
        assertEquals(10, ledger.getQuantityBefore());
        assertEquals(15, ledger.getQuantityAfter());
    }

    @Test
    public void testStockOut_ShouldDecreaseQuantityAndCreateLedger() {
        StockMovementRequest request = new StockMovementRequest();
        request.setVariantId(variant.getId());
        request.setQuantity(3);
        request.setNotes("Damaged goods");

        LedgerEntryDto ledger = inventoryService.stockOut(store.getId(), request, merchant.getId());

        ProductVariant updated = variantRepository.findById(variant.getId()).orElseThrow();
        assertEquals(7, updated.getQuantityOnHand());
        assertEquals(MovementType.STOCK_OUT, ledger.getMovementType());
        assertEquals(10, ledger.getQuantityBefore());
        assertEquals(7, ledger.getQuantityAfter());
    }

    @Test
    public void testStockOut_InsufficientStock_ShouldFail() {
        StockMovementRequest request = new StockMovementRequest();
        request.setVariantId(variant.getId());
        request.setQuantity(15);
        request.setNotes("Large order");

        assertThrows(BusinessRuleException.class, () -> {
            inventoryService.stockOut(store.getId(), request, merchant.getId());
        });

        ProductVariant updated = variantRepository.findById(variant.getId()).orElseThrow();
        assertEquals(10, updated.getQuantityOnHand()); // Unchanged
    }

    @Test
    public void testAdjustStock_ShouldSetExactQuantity() {
        StockAdjustmentRequest request = new StockAdjustmentRequest();
        request.setVariantId(variant.getId());
        request.setNewQuantity(25);
        request.setReason("Audit correction");

        LedgerEntryDto ledger = inventoryService.adjustStock(store.getId(), request, merchant.getId());

        ProductVariant updated = variantRepository.findById(variant.getId()).orElseThrow();
        assertEquals(25, updated.getQuantityOnHand());
        assertEquals(MovementType.ADJUSTMENT, ledger.getMovementType());
        assertEquals(10, ledger.getQuantityBefore());
        assertEquals(25, ledger.getQuantityAfter());
        assertEquals(15, ledger.getQuantity()); // Delta
    }
}
