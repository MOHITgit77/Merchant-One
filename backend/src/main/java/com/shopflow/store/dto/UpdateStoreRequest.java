package com.shopflow.store.dto;

import com.shopflow.store.ShopCategory;
import jakarta.validation.constraints.*;

public class UpdateStoreRequest {
    @NotBlank(message = "Store name is required")
    @Size(min = 2, max = 100, message = "Store name must be between 2 and 100 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    private ShopCategory shopCategory;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    private Double latitude;
    private Double longitude;

    @Pattern(regexp = "^$|^[+]?[0-9]{7,15}$", message = "Invalid phone number format")
    private String phone;

    @Email(message = "Invalid email format")
    private String email;

    private boolean pickupEnabled;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public ShopCategory getShopCategory() { return shopCategory; }
    public void setShopCategory(ShopCategory shopCategory) { this.shopCategory = shopCategory; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public boolean isPickupEnabled() { return pickupEnabled; }
    public void setPickupEnabled(boolean pickupEnabled) { this.pickupEnabled = pickupEnabled; }
}
