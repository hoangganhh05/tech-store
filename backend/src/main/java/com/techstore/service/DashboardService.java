package com.techstore.service;

import com.techstore.dto.response.AdminDashboardResponse;

import java.time.LocalDate;

public interface DashboardService {

    AdminDashboardResponse getAdminDashboard(String period, LocalDate date);
}
