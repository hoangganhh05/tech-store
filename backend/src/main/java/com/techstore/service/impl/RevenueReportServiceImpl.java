package com.techstore.service.impl;

import com.techstore.dto.response.RevenueReportDayResponse;
import com.techstore.dto.response.RevenueReportResponse;
import com.techstore.entity.Order;
import com.techstore.enums.ErrorCode;
import com.techstore.exception.BusinessException;
import com.techstore.repository.OrderRepository;
import com.techstore.service.RevenueReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
