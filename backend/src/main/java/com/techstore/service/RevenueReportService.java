package com.techstore.service;

import com.techstore.dto.response.RevenueReportResponse;

import java.time.LocalDate;

public interface RevenueReportService {

    RevenueReportResponse getRevenueReport(LocalDate fromDate, LocalDate toDate);

    byte[] exportRevenueReport(LocalDate fromDate, LocalDate toDate);
}
