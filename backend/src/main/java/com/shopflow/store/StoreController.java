package com.shopflow.store;

import com.shopflow.common.ApiResponse;
import com.shopflow.security.SecurityUtils;
import com.shopflow.store.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/stores")
@Tag(name = "Stores", description = "Store management")
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @PostMapping
    @Operation(summary = "Create a new store")
    public ResponseEntity<ApiResponse<StoreDto>> createStore(@Valid @RequestBody CreateStoreRequest request) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        StoreDto store = storeService.createStore(request, merchantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(store, "Store created successfully"));
    }

    @GetMapping
    @Operation(summary = "Get all stores for the current merchant")
    public ResponseEntity<ApiResponse<List<StoreDto>>> getStores() {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        List<StoreDto> stores = storeService.getMerchantStores(merchantId);
        return ResponseEntity.ok(ApiResponse.success(stores));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get store by ID")
    public ResponseEntity<ApiResponse<StoreDto>> getStore(@PathVariable UUID id) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        StoreDto store = storeService.getStore(id, merchantId);
        return ResponseEntity.ok(ApiResponse.success(store));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update store")
    public ResponseEntity<ApiResponse<StoreDto>> updateStore(
            @PathVariable UUID id, @Valid @RequestBody UpdateStoreRequest request) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        StoreDto store = storeService.updateStore(id, request, merchantId);
        return ResponseEntity.ok(ApiResponse.success(store, "Store updated successfully"));
    }

    @PatchMapping("/{id}/publish")
    @Operation(summary = "Publish store")
    public ResponseEntity<ApiResponse<StoreDto>> publishStore(@PathVariable UUID id) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        StoreDto store = storeService.publishStore(id, merchantId);
        return ResponseEntity.ok(ApiResponse.success(store, "Store published successfully"));
    }

    @PatchMapping("/{id}/unpublish")
    @Operation(summary = "Unpublish store")
    public ResponseEntity<ApiResponse<StoreDto>> unpublishStore(@PathVariable UUID id) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        StoreDto store = storeService.unpublishStore(id, merchantId);
        return ResponseEntity.ok(ApiResponse.success(store, "Store unpublished successfully"));
    }

    // ========== Business Hours ==========

    @GetMapping("/{id}/hours")
    @Operation(summary = "Get store business hours")
    public ResponseEntity<ApiResponse<List<StoreHoursDto>>> getHours(@PathVariable UUID id) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        List<StoreHoursDto> hours = storeService.getHours(id, merchantId);
        return ResponseEntity.ok(ApiResponse.success(hours));
    }

    @PutMapping("/{id}/hours")
    @Operation(summary = "Update store business hours")
    public ResponseEntity<ApiResponse<List<StoreHoursDto>>> updateHours(
            @PathVariable UUID id, @RequestBody List<StoreHoursDto> hours) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        List<StoreHoursDto> updated = storeService.updateHours(id, hours, merchantId);
        return ResponseEntity.ok(ApiResponse.success(updated, "Business hours updated"));
    }

    // ========== Holidays ==========

    @GetMapping("/{id}/holidays")
    @Operation(summary = "Get store holidays")
    public ResponseEntity<ApiResponse<List<StoreHolidayDto>>> getHolidays(@PathVariable UUID id) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        List<StoreHolidayDto> holidays = storeService.getHolidays(id, merchantId);
        return ResponseEntity.ok(ApiResponse.success(holidays));
    }

    @PostMapping("/{id}/holidays")
    @Operation(summary = "Add a holiday")
    public ResponseEntity<ApiResponse<StoreHolidayDto>> addHoliday(
            @PathVariable UUID id, @Valid @RequestBody StoreHolidayDto dto) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        StoreHolidayDto holiday = storeService.addHoliday(id, dto, merchantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(holiday, "Holiday added"));
    }

    @DeleteMapping("/{id}/holidays/{holidayId}")
    @Operation(summary = "Delete a holiday")
    public ResponseEntity<ApiResponse<Void>> deleteHoliday(
            @PathVariable UUID id, @PathVariable UUID holidayId) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        storeService.deleteHoliday(id, holidayId, merchantId);
        return ResponseEntity.ok(ApiResponse.message("Holiday deleted"));
    }

    // ========== Delivery Settings ==========

    @GetMapping("/{id}/delivery")
    @Operation(summary = "Get delivery settings")
    public ResponseEntity<ApiResponse<DeliverySettingsDto>> getDelivery(@PathVariable UUID id) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        DeliverySettingsDto delivery = storeService.getDeliverySettings(id, merchantId);
        return ResponseEntity.ok(ApiResponse.success(delivery));
    }

    @PutMapping("/{id}/delivery")
    @Operation(summary = "Update delivery settings")
    public ResponseEntity<ApiResponse<DeliverySettingsDto>> updateDelivery(
            @PathVariable UUID id, @Valid @RequestBody DeliverySettingsDto dto) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        DeliverySettingsDto delivery = storeService.updateDeliverySettings(id, dto, merchantId);
        return ResponseEntity.ok(ApiResponse.success(delivery, "Delivery settings updated"));
    }

    // ========== Payment Preferences ==========

    @GetMapping("/{id}/payments")
    @Operation(summary = "Get payment preferences")
    public ResponseEntity<ApiResponse<List<PaymentPreferenceDto>>> getPayments(@PathVariable UUID id) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        List<PaymentPreferenceDto> prefs = storeService.getPaymentPreferences(id, merchantId);
        return ResponseEntity.ok(ApiResponse.success(prefs));
    }

    @PutMapping("/{id}/payments")
    @Operation(summary = "Update payment preferences")
    public ResponseEntity<ApiResponse<List<PaymentPreferenceDto>>> updatePayments(
            @PathVariable UUID id, @RequestBody List<PaymentPreferenceDto> dtos) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        List<PaymentPreferenceDto> prefs = storeService.updatePaymentPreferences(id, dtos, merchantId);
        return ResponseEntity.ok(ApiResponse.success(prefs, "Payment preferences updated"));
    }

    // ========== File Uploads ==========

    @PostMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload store logo")
    public ResponseEntity<ApiResponse<StoreDto>> uploadLogo(
            @PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        StoreDto store = storeService.uploadLogo(id, file, merchantId);
        return ResponseEntity.ok(ApiResponse.success(store, "Logo uploaded successfully"));
    }

    @PostMapping(value = "/{id}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload store cover image")
    public ResponseEntity<ApiResponse<StoreDto>> uploadCover(
            @PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        UUID merchantId = SecurityUtils.getCurrentUserId();
        StoreDto store = storeService.uploadCoverImage(id, file, merchantId);
        return ResponseEntity.ok(ApiResponse.success(store, "Cover image uploaded successfully"));
    }
}
