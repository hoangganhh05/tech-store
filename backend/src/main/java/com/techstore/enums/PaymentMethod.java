package com.techstore.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum PaymentMethod {
    COD, BANK_TRANSFER, ONLINE;

    @JsonCreator
    public static PaymentMethod fromValue(String value) {
        return valueOf(value);
    }
}
