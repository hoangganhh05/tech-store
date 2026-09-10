package com.techstore.controller.admin;

import com.techstore.dto.response.ApiResponse;
import com.techstore.dto.response.RevenueReportResponse;
import com.techstore.enums.RoleCode;
import com.techstore.security.RequireRole;
import com.techstore.service.RevenueReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("${app.api.base-path}/admin/reports/revenue")
@RequireRole(RoleCode.ADMIN)
@Tag(name = "Admin Revenue Reports", description = "Báo cáo doanh thu theo khoảng thời gian")
public class AdminRevenueReportController {

    private final RevenueReportService revenueReportService;

    public AdminRevenueReportController(RevenueReportService revenueReportService) {
        this.revenueReportService = revenueReportService;
    }

    @GetMapping
    @Operation(summary = "Xem báo cáo doanh thu chi tiết theo khoảng ngày tùy chọn")
    public ApiResponse<RevenueReportResponse> getRevenueReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ApiResponse.success("Lấy báo cáo doanh thu thành công",
                revenueReportService.getRevenueReport(fromDate, toDate));
    }

    @GetMapping("/export")
    @Operation(summary = "Xuất báo cáo doanh thu và đơn hàng ra file Excel")
    public ResponseEntity<byte[]> exportRevenueReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        byte[] content = revenueReportService.exportRevenueReport(fromDate, toDate);
        String filename = "bao-cao-doanh-thu-" + fromDate + "-den-" + toDate + ".xlsx";
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentLength(content.length)
                .body(content);
    }
}
