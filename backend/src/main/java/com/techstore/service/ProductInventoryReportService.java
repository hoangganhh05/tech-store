package com.techstore.service;

import com.techstore.dto.response.ProductInventoryReportResponse;

import java.time.LocalDate;

public interface ProductInventoryReportService {

    ProductInventoryReportResponse getReport(LocalDate fromDate, LocalDate toDate,
                                             Long categoryId, String sortBy, String sortDirection);
}
