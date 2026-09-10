package com.techstore.service.impl;

import com.techstore.dto.response.RevenueReportDayResponse;
import com.techstore.dto.response.RevenueReportResponse;
import com.techstore.entity.Order;
import com.techstore.enums.ErrorCode;
import com.techstore.exception.BusinessException;
import com.techstore.repository.OrderRepository;
import com.techstore.service.RevenueReportService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.poi.ss.util.CellRangeAddress;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RevenueReportServiceImpl implements RevenueReportService {

    private static final ZoneOffset REPORT_ZONE = ZoneOffset.UTC;

    private final OrderRepository orders;

    public RevenueReportServiceImpl(OrderRepository orders) {
        this.orders = orders;
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueReportResponse getRevenueReport(LocalDate fromDate, LocalDate toDate) {
        validateRange(fromDate, toDate);

        Instant fromInclusive = fromDate.atStartOfDay().toInstant(REPORT_ZONE);
        Instant toExclusive = toDate.plusDays(1).atStartOfDay().toInstant(REPORT_ZONE);
        List<Order> validOrders = orders.findValidOrdersForRevenueReport(fromInclusive, toExclusive);

        Map<LocalDate, DailyAccumulator> daily = createDailyBuckets(fromDate, toDate);
        BigDecimal totalRevenue = BigDecimal.ZERO;
        for (Order order : validOrders) {
            BigDecimal orderRevenue = moneyValue(order.getTotalAmount());
            totalRevenue = totalRevenue.add(orderRevenue);
            LocalDate orderDate = order.getPlacedAt().atZone(REPORT_ZONE).toLocalDate();
            DailyAccumulator accumulator = daily.get(orderDate);
            if (accumulator != null) {
                accumulator.orderCount++;
                accumulator.revenue = accumulator.revenue.add(orderRevenue);
            }
        }

        List<RevenueReportDayResponse> dailyRevenue = new ArrayList<>(daily.size());
        daily.forEach((date, accumulator) -> dailyRevenue.add(new RevenueReportDayResponse(
                date,
                money(accumulator.revenue),
                accumulator.orderCount,
                average(accumulator.revenue, accumulator.orderCount)
        )));

        return new RevenueReportResponse(
                fromDate,
                toDate,
                money(totalRevenue),
                validOrders.size(),
                average(totalRevenue, validOrders.size()),
                dailyRevenue
        );
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportRevenueReport(LocalDate fromDate, LocalDate toDate) {
        RevenueReportResponse report = getRevenueReport(fromDate, toDate);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Báo cáo doanh thu");
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor((short) 22);
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));

            Row titleRow = sheet.createRow(0);
            Cell title = titleRow.createCell(0);
            title.setCellValue("BÁO CÁO DOANH THU / ĐƠN HÀNG");
            title.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

            Row rangeRow = sheet.createRow(1);
            rangeRow.createCell(0).setCellValue("Khoảng thời gian");
            rangeRow.createCell(1).setCellValue(report.fromDate() + " - " + report.toDate());

            Row totalRow = sheet.createRow(2);
            totalRow.createCell(0).setCellValue("Tổng doanh thu");
            setNumber(totalRow.createCell(1), report.totalRevenue(), currencyStyle);
            totalRow.createCell(2).setCellValue("Tổng số đơn hàng");
            totalRow.createCell(3).setCellValue(report.totalOrders());

            Row averageRow = sheet.createRow(3);
            averageRow.createCell(0).setCellValue("Giá trị đơn trung bình");
            setNumber(averageRow.createCell(1), report.averageOrderValue(), currencyStyle);

            Row headerRow = sheet.createRow(5);
            String[] headers = {"Ngày", "Doanh thu (VND)", "Số đơn hàng", "Giá trị đơn trung bình (VND)"};
            for (int index = 0; index < headers.length; index++) {
                Cell cell = headerRow.createCell(index);
                cell.setCellValue(headers[index]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 6;
            for (var day : report.dailyRevenue()) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(day.date().toString());
                setNumber(row.createCell(1), day.revenue(), currencyStyle);
                row.createCell(2).setCellValue(day.orderCount());
                setNumber(row.createCell(3), day.averageOrderValue(), currencyStyle);
            }

            sheet.setAutoFilter(new CellRangeAddress(5, Math.max(5, rowIndex - 1), 0, 3));
            sheet.createFreezePane(0, 6);
            sheet.setColumnWidth(0, 18 * 256);
            sheet.setColumnWidth(1, 22 * 256);
            sheet.setColumnWidth(2, 18 * 256);
            sheet.setColumnWidth(3, 32 * 256);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.FILE_EXPORT_ERROR,
                    "Không thể tạo file báo cáo doanh thu");
        }
    }

    private void setNumber(Cell cell, BigDecimal value, CellStyle style) {
        cell.setCellValue(value.doubleValue());
        cell.setCellStyle(style);
    }

    private void validateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Ngày bắt đầu và ngày kết thúc là bắt buộc");
        }
        if (fromDate.isAfter(toDate)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Ngày bắt đầu không được sau ngày kết thúc");
        }
    }

    private Map<LocalDate, DailyAccumulator> createDailyBuckets(LocalDate fromDate, LocalDate toDate) {
        Map<LocalDate, DailyAccumulator> buckets = new LinkedHashMap<>();
        LocalDate cursor = fromDate;
        while (!cursor.isAfter(toDate)) {
            buckets.put(cursor, new DailyAccumulator());
            cursor = cursor.plusDays(1);
        }
        return buckets;
    }

    private BigDecimal average(BigDecimal value, long count) {
        if (count == 0) return money(BigDecimal.ZERO);
        return money(value.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP));
    }

    private BigDecimal moneyValue(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal money(BigDecimal value) {
        return moneyValue(value).setScale(2, RoundingMode.HALF_UP);
    }

    private static final class DailyAccumulator {
        private BigDecimal revenue = BigDecimal.ZERO;
        private long orderCount;
    }
}
