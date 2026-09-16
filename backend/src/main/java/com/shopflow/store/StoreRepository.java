package com.shopflow.store;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StoreRepository extends JpaRepository<Store, UUID> {
    List<Store> findByMerchantId(UUID merchantId);
    Optional<Store> findBySlug(String slug);
    boolean existsBySlug(String slug);
    Optional<Store> findByIdAndMerchantId(UUID id, UUID merchantId);
    Optional<Store> findBySlugAndIsPublishedTrue(String slug);
}
