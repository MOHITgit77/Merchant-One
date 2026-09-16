package com.shopflow.store.dto;

import com.shopflow.store.PaymentMethod;

public class PaymentPreferenceDto {
    private PaymentMethod paymentMethod;
    private boolean enabled;

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public static PaymentPreferenceDto fromEntity(com.shopflow.store.PaymentPreference pref) {
        PaymentPreferenceDto dto = new PaymentPreferenceDto();
        dto.setPaymentMethod(pref.getPaymentMethod());
        dto.setEnabled(pref.isEnabled());
        return dto;
    }
}
