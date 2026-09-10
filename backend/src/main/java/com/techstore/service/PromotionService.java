package com.techstore.service;

import com.techstore.dto.request.PromotionRequest;
import com.techstore.dto.response.PageResponse;
import com.techstore.dto.response.PromotionResponse;
import com.techstore.entity.ProductVariant;

import java.util.Collection;
import java.util.Map;

public interface PromotionService {
    PromotionResponse create(PromotionRequest request);
    PageResponse<PromotionResponse> getAll(int page, int size);
    PromotionResponse getById(Long id);
    PromotionResponse update(Long id, PromotionRequest request);
    void delete(Long id);
    Map<Long, EffectivePrice> getEffectivePrices(Collection<ProductVariant> variants);
}
