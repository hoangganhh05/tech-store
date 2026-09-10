package com.techstore.service;

import com.techstore.dto.request.VoucherRequest;
import com.techstore.dto.response.PageResponse;
import com.techstore.dto.response.VoucherResponse;

public interface VoucherManagementService {
    VoucherResponse create(VoucherRequest request);
    PageResponse<VoucherResponse> getAll(String keyword, int page, int size);
    VoucherResponse getById(Long id);
    VoucherResponse update(Long id, VoucherRequest request);
    void delete(Long id);
}
