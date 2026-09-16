package com.shopflow.store;

import com.shopflow.store.dto.CreateStoreRequest;
import com.shopflow.store.dto.StoreDto;
import com.shopflow.user.User;
import com.shopflow.user.UserRepository;
import com.shopflow.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class StoreServiceTest {

    @Autowired private StoreService storeService;
    @Autowired private StoreRepository storeRepository;
    @Autowired private UserRepository userRepository;

    private User merchant;

    @BeforeEach
    public void setup() {
        storeRepository.deleteAll();
        userRepository.deleteAll();

        merchant = new User();
        merchant.setName("Store Merchant");
        merchant.setEmail("store" + System.currentTimeMillis() + "@merchant.com");
        merchant.setPasswordHash("hash");
        merchant.setRole(UserRole.MERCHANT);
        merchant = userRepository.save(merchant);
    }

    @Test
    public void testCreateStore_ShouldSaveStore() {
        CreateStoreRequest request = new CreateStoreRequest();
        request.setName("My Shop");
        request.setShopCategory(ShopCategory.GROCERY);

        StoreDto saved = storeService.createStore(request, merchant.getId());
        assertNotNull(saved.getId());
        assertEquals("My Shop", saved.getName());
    }

    @Test
    public void testVerifyStoreOwnership_ShouldPassForOwnerAndFailForOther() {
        CreateStoreRequest request = new CreateStoreRequest();
        request.setName("My Shop");
        request.setShopCategory(ShopCategory.GROCERY);
        StoreDto saved = storeService.createStore(request, merchant.getId());

        User otherMerchant = new User();
        otherMerchant.setName("Other");
        otherMerchant.setEmail("other" + System.currentTimeMillis() + "@merchant.com");
        otherMerchant.setPasswordHash("hash");
        otherMerchant.setRole(UserRole.MERCHANT);
        final User finalOtherMerchant = userRepository.save(otherMerchant);

        // Owner succeeds
        assertDoesNotThrow(() -> storeService.verifyStoreOwnership(saved.getId(), merchant.getId()));

        // Other fails
        assertThrows(AccessDeniedException.class, () -> storeService.verifyStoreOwnership(saved.getId(), finalOtherMerchant.getId()));
    }
    @Test
    public void testCreateMultipleStores_ShouldBelongToSameMerchant() {
        CreateStoreRequest request1 = new CreateStoreRequest();
        request1.setName("My Shop 1");
        request1.setShopCategory(ShopCategory.GROCERY);
        StoreDto saved1 = storeService.createStore(request1, merchant.getId());

        CreateStoreRequest request2 = new CreateStoreRequest();
        request2.setName("My Shop 2");
        request2.setShopCategory(ShopCategory.ELECTRONICS);
        StoreDto saved2 = storeService.createStore(request2, merchant.getId());

        java.util.List<StoreDto> stores = storeService.getMerchantStores(merchant.getId());
        assertEquals(2, stores.size());
        assertTrue(stores.stream().anyMatch(s -> s.getId().equals(saved1.getId())));
        assertTrue(stores.stream().anyMatch(s -> s.getId().equals(saved2.getId())));
    }

    @Test
    public void testGenerateUniqueSlug_ShouldHandleDuplicateNames() {
        CreateStoreRequest request1 = new CreateStoreRequest();
        request1.setName("Duplicate Name");
        request1.setShopCategory(ShopCategory.GROCERY);
        StoreDto saved1 = storeService.createStore(request1, merchant.getId());

        CreateStoreRequest request2 = new CreateStoreRequest();
        request2.setName("Duplicate Name");
        request2.setShopCategory(ShopCategory.GROCERY);
        StoreDto saved2 = storeService.createStore(request2, merchant.getId());

        assertEquals("duplicate-name", saved1.getSlug());
        assertEquals("duplicate-name-2", saved2.getSlug());
    }
}
