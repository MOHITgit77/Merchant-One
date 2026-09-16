package com.shopflow.store;

import com.github.slugify.Slugify;
import com.shopflow.exception.BusinessRuleException;
import com.shopflow.exception.InvalidStateTransitionException;
import com.shopflow.exception.ResourceNotFoundException;
import com.shopflow.store.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class StoreService {

    private static final Logger log = LoggerFactory.getLogger(StoreService.class);
    private final Slugify slugify = Slugify.builder().lowerCase(true).build();

    private final StoreRepository storeRepository;
    private final StoreHoursRepository storeHoursRepository;
    private final StoreHolidayRepository storeHolidayRepository;
    private final DeliverySettingsRepository deliverySettingsRepository;
    private final PaymentPreferenceRepository paymentPreferenceRepository;
    private final FileStorageService fileStorageService;

    public StoreService(StoreRepository storeRepository,
                        StoreHoursRepository storeHoursRepository,
                        StoreHolidayRepository storeHolidayRepository,
                        DeliverySettingsRepository deliverySettingsRepository,
                        PaymentPreferenceRepository paymentPreferenceRepository,
                        FileStorageService fileStorageService) {
        this.storeRepository = storeRepository;
        this.storeHoursRepository = storeHoursRepository;
        this.storeHolidayRepository = storeHolidayRepository;
        this.deliverySettingsRepository = deliverySettingsRepository;
        this.paymentPreferenceRepository = paymentPreferenceRepository;
        this.fileStorageService = fileStorageService;
    }

    // ========== Ownership verification ==========

    public Store getStoreForMerchant(UUID storeId, UUID merchantId) {
        return storeRepository.findByIdAndMerchantId(storeId, merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", "id", storeId));
    }

    public void verifyStoreOwnership(UUID storeId, UUID merchantId) {
        if (storeRepository.findByIdAndMerchantId(storeId, merchantId).isEmpty()) {
            throw new AccessDeniedException("You do not have access to this store");
        }
    }

    // ========== Store CRUD ==========

    @Transactional
    public StoreDto createStore(CreateStoreRequest request, UUID merchantId) {
        Store store = new Store();
        store.setMerchantId(merchantId);
        store.setName(request.getName());
        store.setSlug(generateUniqueSlug(request.getName()));
        store.setDescription(request.getDescription());
        store.setShopCategory(request.getShopCategory());
        store.setAddress(request.getAddress());
        store.setLatitude(request.getLatitude());
        store.setLongitude(request.getLongitude());
        store.setPhone(request.getPhone());
        store.setEmail(request.getEmail());
        store.setPickupEnabled(request.isPickupEnabled());

        store = storeRepository.save(store);

        // Initialize default business hours (Mon–Sat open, Sun closed)
        initializeDefaultHours(store.getId());

        // Initialize default delivery settings
        DeliverySettings ds = new DeliverySettings();
        ds.setStoreId(store.getId());
        deliverySettingsRepository.save(ds);

        // Initialize default payment preferences
        for (PaymentMethod method : PaymentMethod.values()) {
            PaymentPreference pref = new PaymentPreference();
            pref.setStoreId(store.getId());
            pref.setPaymentMethod(method);
            pref.setEnabled(method == PaymentMethod.CASH);
            paymentPreferenceRepository.save(pref);
        }

        log.info("Store created: {} (slug: {})", store.getName(), store.getSlug());
        return StoreDto.fromEntity(store);
    }

    @Transactional
    public StoreDto updateStore(UUID storeId, UpdateStoreRequest request, UUID merchantId) {
        Store store = getStoreForMerchant(storeId, merchantId);

        store.setName(request.getName());
        // Slug is NOT changed when name is updated — URL stability
        store.setDescription(request.getDescription());
        store.setShopCategory(request.getShopCategory());
        store.setAddress(request.getAddress());
        store.setLatitude(request.getLatitude());
        store.setLongitude(request.getLongitude());
        store.setPhone(request.getPhone());
        store.setEmail(request.getEmail());
        store.setPickupEnabled(request.isPickupEnabled());

        store = storeRepository.save(store);
        log.info("Store updated: {}", store.getId());
        return StoreDto.fromEntity(store);
    }

    @Transactional(readOnly = true)
    public StoreDto getStore(UUID storeId, UUID merchantId) {
        Store store = getStoreForMerchant(storeId, merchantId);
        return StoreDto.fromEntity(store);
    }

    @Transactional(readOnly = true)
    public List<StoreDto> getMerchantStores(UUID merchantId) {
        return storeRepository.findByMerchantId(merchantId).stream()
                .map(StoreDto::fromEntity)
                .collect(Collectors.toList());
    }

    // ========== Publish / Unpublish ==========

    @Transactional
    public StoreDto publishStore(UUID storeId, UUID merchantId) {
        Store store = getStoreForMerchant(storeId, merchantId);
        if (store.isPublished()) {
            throw new InvalidStateTransitionException("Store is already published");
        }
        // Validate minimum requirements for publishing
        if (store.getName() == null || store.getName().isBlank()) {
            throw new BusinessRuleException("INCOMPLETE_STORE", "Store name is required before publishing");
        }
        store.setPublished(true);
        store = storeRepository.save(store);
        log.info("Store published: {}", store.getSlug());
        return StoreDto.fromEntity(store);
    }

    @Transactional
    public StoreDto unpublishStore(UUID storeId, UUID merchantId) {
        Store store = getStoreForMerchant(storeId, merchantId);
        if (!store.isPublished()) {
            throw new InvalidStateTransitionException("Store is already unpublished");
        }
        store.setPublished(false);
        store = storeRepository.save(store);
        log.info("Store unpublished: {}", store.getSlug());
        return StoreDto.fromEntity(store);
    }

    // ========== Business Hours ==========

    @Transactional
    public List<StoreHoursDto> updateHours(UUID storeId, List<StoreHoursDto> hoursDtos, UUID merchantId) {
        Store store = getStoreForMerchant(storeId, merchantId);

        // Validate hours
        for (StoreHoursDto dto : hoursDtos) {
            if (!dto.isClosed() && dto.getOpeningTime() != null && dto.getClosingTime() != null) {
                if (!dto.getClosingTime().isAfter(dto.getOpeningTime())) {
                    throw new BusinessRuleException("INVALID_HOURS",
                            "Closing time must be after opening time for " + dto.getDayOfWeek());
                }
            }
        }

        storeHoursRepository.deleteByStoreId(store.getId());
        storeHoursRepository.flush();

        for (StoreHoursDto dto : hoursDtos) {
            StoreHours hours = new StoreHours();
            hours.setStoreId(store.getId());
            hours.setDayOfWeek(dto.getDayOfWeek());
            hours.setOpeningTime(dto.getOpeningTime());
            hours.setClosingTime(dto.getClosingTime());
            hours.setClosed(dto.isClosed());
            storeHoursRepository.save(hours);
        }

        return getHours(storeId, merchantId);
    }

    @Transactional(readOnly = true)
    public List<StoreHoursDto> getHours(UUID storeId, UUID merchantId) {
        verifyStoreOwnership(storeId, merchantId);
        return storeHoursRepository.findByStoreIdOrderByDayOfWeek(storeId).stream()
                .map(StoreHoursDto::fromEntity)
                .collect(Collectors.toList());
    }

    // ========== Holidays ==========

    @Transactional
    public StoreHolidayDto addHoliday(UUID storeId, StoreHolidayDto dto, UUID merchantId) {
        verifyStoreOwnership(storeId, merchantId);
        StoreHoliday holiday = new StoreHoliday();
        holiday.setStoreId(storeId);
        holiday.setHolidayDate(dto.getHolidayDate());
        holiday.setReason(dto.getReason());
        holiday = storeHolidayRepository.save(holiday);
        return StoreHolidayDto.fromEntity(holiday);
    }

    @Transactional(readOnly = true)
    public List<StoreHolidayDto> getHolidays(UUID storeId, UUID merchantId) {
        verifyStoreOwnership(storeId, merchantId);
        return storeHolidayRepository.findByStoreIdOrderByHolidayDate(storeId).stream()
                .map(StoreHolidayDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteHoliday(UUID storeId, UUID holidayId, UUID merchantId) {
        verifyStoreOwnership(storeId, merchantId);
        StoreHoliday holiday = storeHolidayRepository.findById(holidayId)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday", "id", holidayId));
        if (!holiday.getStoreId().equals(storeId)) {
            throw new AccessDeniedException("Holiday does not belong to this store");
        }
        storeHolidayRepository.delete(holiday);
    }

    // ========== Delivery Settings ==========

    @Transactional
    public DeliverySettingsDto updateDeliverySettings(UUID storeId, DeliverySettingsDto dto, UUID merchantId) {
        verifyStoreOwnership(storeId, merchantId);
        DeliverySettings ds = deliverySettingsRepository.findByStoreId(storeId)
                .orElseGet(() -> {
                    DeliverySettings newDs = new DeliverySettings();
                    newDs.setStoreId(storeId);
                    return newDs;
                });
        ds.setEnabled(dto.isEnabled());
        ds.setDeliveryRadius(dto.getDeliveryRadius());
        ds.setDeliveryFee(dto.getDeliveryFee());
        ds.setMinimumOrder(dto.getMinimumOrder());
        ds.setFreeDeliveryThreshold(dto.getFreeDeliveryThreshold());
        ds = deliverySettingsRepository.save(ds);
        return DeliverySettingsDto.fromEntity(ds);
    }

    @Transactional(readOnly = true)
    public DeliverySettingsDto getDeliverySettings(UUID storeId, UUID merchantId) {
        verifyStoreOwnership(storeId, merchantId);
        return deliverySettingsRepository.findByStoreId(storeId)
                .map(DeliverySettingsDto::fromEntity)
                .orElse(new DeliverySettingsDto());
    }

    // ========== Payment Preferences ==========

    @Transactional
    public List<PaymentPreferenceDto> updatePaymentPreferences(UUID storeId,
            List<PaymentPreferenceDto> dtos, UUID merchantId) {
        verifyStoreOwnership(storeId, merchantId);
        paymentPreferenceRepository.deleteByStoreId(storeId);
        paymentPreferenceRepository.flush();

        for (PaymentPreferenceDto dto : dtos) {
            PaymentPreference pref = new PaymentPreference();
            pref.setStoreId(storeId);
            pref.setPaymentMethod(dto.getPaymentMethod());
            pref.setEnabled(dto.isEnabled());
            paymentPreferenceRepository.save(pref);
        }

        return getPaymentPreferences(storeId, merchantId);
    }

    @Transactional(readOnly = true)
    public List<PaymentPreferenceDto> getPaymentPreferences(UUID storeId, UUID merchantId) {
        verifyStoreOwnership(storeId, merchantId);
        return paymentPreferenceRepository.findByStoreId(storeId).stream()
                .map(PaymentPreferenceDto::fromEntity)
                .collect(Collectors.toList());
    }

    // ========== File Uploads ==========

    @Transactional
    public StoreDto uploadLogo(UUID storeId, MultipartFile file, UUID merchantId) {
        Store store = getStoreForMerchant(storeId, merchantId);
        String url = fileStorageService.storeFile(file, "stores/" + storeId + "/logo");
        store.setLogoUrl(url);
        store = storeRepository.save(store);
        return StoreDto.fromEntity(store);
    }

    @Transactional
    public StoreDto uploadCoverImage(UUID storeId, MultipartFile file, UUID merchantId) {
        Store store = getStoreForMerchant(storeId, merchantId);
        String url = fileStorageService.storeFile(file, "stores/" + storeId + "/cover");
        store.setCoverImageUrl(url);
        store = storeRepository.save(store);
        return StoreDto.fromEntity(store);
    }

    // ========== Slug Generation ==========

    String generateUniqueSlug(String name) {
        String baseSlug = slugify.slugify(name);
        if (baseSlug == null || baseSlug.isBlank()) {
            baseSlug = "store";
        }

        String candidateSlug = baseSlug;
        int suffix = 2;
        while (storeRepository.existsBySlug(candidateSlug)) {
            candidateSlug = baseSlug + "-" + suffix;
            suffix++;
        }
        return candidateSlug;
    }

    // ========== Helpers ==========

    private void initializeDefaultHours(UUID storeId) {
        for (DayOfWeek day : DayOfWeek.values()) {
            StoreHours hours = new StoreHours();
            hours.setStoreId(storeId);
            hours.setDayOfWeek(day);
            if (day == DayOfWeek.SUNDAY) {
                hours.setClosed(true);
            } else {
                hours.setOpeningTime(java.time.LocalTime.of(9, 0));
                hours.setClosingTime(java.time.LocalTime.of(21, 0));
                hours.setClosed(false);
            }
            storeHoursRepository.save(hours);
        }
    }
}
