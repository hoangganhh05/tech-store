package com.techstore.service;

import java.math.BigDecimal;

public record EffectivePrice(BigDecimal price, BigDecimal originalPrice, int promotionPercent) {
}
