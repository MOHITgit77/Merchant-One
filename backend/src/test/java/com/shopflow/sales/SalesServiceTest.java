package com.shopflow.sales;

import com.shopflow.exception.BusinessRuleException;
import com.shopflow.inventory.InventoryLedgerRepository;
import com.shopflow.inventory.InventoryService;
import com.shopflow.product.Product;
import com.shopflow.product.ProductRepository;
import com.shopflow.product.ProductUnit;
import com.shopflow.product.ProductVariant;
import com.shopflow.product.ProductVariantRepository;
import com.shopflow.sales.dto.CreateSaleRequest;
import com.shopflow.sales.dto.SaleDto;
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
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class SalesServiceTest {

    @Autowired private SalesService salesService;
    @Autowired private StoreRepository storeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository variantRepository;
    @Autowired private InventoryLedgerRepository ledgerRepository;
    @Autowired private SaleRepository saleRepository;

    private User merchant;
    private Store store;
    private Product product;
    private ProductVariant variant;

    @BeforeEach
    public void setup() {
        // Clear all to avoid unique constraint issues if data lingers
        ledgerRepository.deleteAll();
        saleRepository.deleteAll();
        variantRepository.deleteAll();
        productRepository.deleteAll();
        storeRepository.deleteAll();
        userRepository.deleteAll();

        merchant = new User();
        merchant.setName("Test Merchant");
        merchant.setEmail("test" + System.currentTimeMillis() + "@merchant.com");
        merchant.setPasswordHash("hash");
        merchant.setRole(UserRole.MERCHANT);
        merchant = userRepository.save(merchant);

        store = new Store();
        store.setMerchantId(merchant.getId());
        store.setName("Test Store");
        store.setShopCategory(ShopCategory.GROCERY);
        store.setSlug("test-store-" + System.currentTimeMillis());
        store = storeRepository.save(store);

        product = new Product();
        product.setStoreId(store.getId());
        product.setName("Test Product");
        product.setPrice(BigDecimal.TEN);
        product.setUnit(ProductUnit.PCS);
        product.setTrackInventory(true);
        product = productRepository.save(product);

        variant = new ProductVariant();
        variant.setProduct(product);
        variant.setName("Default");
        variant.setSku("SKU-" + System.currentTimeMillis());
        variant.setPrice(BigDecimal.TEN);
        variant.setQuantityOnHand(2); // Set stock to 2 for testing
        variant = variantRepository.save(variant);
    }

    @Test
    public void test1_InsufficientStock_ShouldFailAndNotUpdateLedger() {
        // Given: Stock = 2
        assertEquals(2, variantRepository.findById(variant.getId()).orElseThrow().getQuantityOnHand());

        // Attempt: Sell = 3
        CreateSaleRequest request = new CreateSaleRequest();
        request.setPaymentMethod(com.shopflow.store.PaymentMethod.CASH);
        
        CreateSaleRequest.SaleItemRequest itemReq = new CreateSaleRequest.SaleItemRequest();
        itemReq.setVariantId(variant.getId());
        itemReq.setQuantity(3);
        request.setItems(List.of(itemReq));

        // Expected: request fails
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> {
            salesService.createSale(store.getId(), request, null, merchant.getId());
        });
        assertEquals("INSUFFICIENT_STOCK", ex.getCode());

        // Expected: sale is NOT created
        assertEquals(0, saleRepository.count());

        // Expected: stock remains 2
        assertEquals(2, variantRepository.findById(variant.getId()).orElseThrow().getQuantityOnHand());

        // Expected: no incorrect inventory ledger movement
        assertEquals(0, ledgerRepository.count());
    }

    @Test
    public void test2_Idempotency_ShouldNotDuplicateSale() {
        CreateSaleRequest request = new CreateSaleRequest();
        request.setPaymentMethod(com.shopflow.store.PaymentMethod.CASH);
        
        CreateSaleRequest.SaleItemRequest itemReq = new CreateSaleRequest.SaleItemRequest();
        itemReq.setVariantId(variant.getId());
        itemReq.setQuantity(1);
        request.setItems(List.of(itemReq));

        String idempotencyKey = "idem-" + System.currentTimeMillis();

        // Send first request
        SaleDto firstResponse = salesService.createSale(store.getId(), request, idempotencyKey, merchant.getId());
        
        // Send exact same request again
        SaleDto secondResponse = salesService.createSale(store.getId(), request, idempotencyKey, merchant.getId());

        // Expected: exact same sale ID returned
        assertEquals(firstResponse.getId(), secondResponse.getId());

        // Expected: exactly one sale created
        assertEquals(1, saleRepository.count());

        // Expected: exactly one inventory deduction (stock goes from 2 to 1)
        assertEquals(1, variantRepository.findById(variant.getId()).orElseThrow().getQuantityOnHand());
        assertEquals(1, ledgerRepository.count());
    }

    @Test
    public void test3_TransactionRollback_OnLedgerFailure() {
        // Force a failure in the middle of a sale creation. 
        // We simulate this by passing a non-existent variant in the same sale.
        // It should rollback the entire transaction.
        
        CreateSaleRequest request = new CreateSaleRequest();
        request.setPaymentMethod(com.shopflow.store.PaymentMethod.CASH);
        
        CreateSaleRequest.SaleItemRequest validItem = new CreateSaleRequest.SaleItemRequest();
        validItem.setVariantId(variant.getId());
        validItem.setQuantity(1);
        
        CreateSaleRequest.SaleItemRequest invalidItem = new CreateSaleRequest.SaleItemRequest();
        invalidItem.setVariantId(java.util.UUID.randomUUID()); // Does not exist
        invalidItem.setQuantity(1);

        request.setItems(List.of(validItem, invalidItem));

        assertThrows(Exception.class, () -> {
            salesService.createSale(store.getId(), request, null, merchant.getId());
        });

        // Expected: sale rolled back
        assertEquals(0, saleRepository.count());

        // Expected: inventory update rolled back (stock remains 2)
        assertEquals(2, variantRepository.findById(variant.getId()).orElseThrow().getQuantityOnHand());

        // Expected: ledger movement rolled back
        assertEquals(0, ledgerRepository.count());
    }

    @Test
    public void test4_ConcurrentSales_ShouldPreventOversellAndLostUpdates() throws InterruptedException {
        // Initial stock = 1
        variant.setQuantityOnHand(1);
        variantRepository.save(variant);

        CreateSaleRequest request = new CreateSaleRequest();
        request.setPaymentMethod(com.shopflow.store.PaymentMethod.CASH);
        CreateSaleRequest.SaleItemRequest itemReq = new CreateSaleRequest.SaleItemRequest();
        itemReq.setVariantId(variant.getId());
        itemReq.setQuantity(1);
        request.setItems(List.of(itemReq));

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger exceptionCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    latch.await(); // wait for start signal
                    salesService.createSale(store.getId(), request, null, merchant.getId());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    exceptionCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Start all threads simultaneously
        latch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);

        // Expected: Only one sale can successfully consume the final unit.
        assertEquals(1, successCount.get());
        assertEquals(1, exceptionCount.get());

        // Inventory must NEVER become negative.
        ProductVariant updatedVariant = variantRepository.findById(variant.getId()).orElseThrow();
        assertEquals(0, updatedVariant.getQuantityOnHand());

        // The database must remain consistent.
        assertEquals(1, saleRepository.count());
        assertEquals(1, ledgerRepository.count());
        
        executor.shutdown();
    }
}
