package com.techstore.service.impl;

import com.techstore.dto.response.AdminDashboardResponse;
import com.techstore.dto.response.DashboardRevenuePointResponse;
import com.techstore.dto.response.DashboardStatusCountResponse;
import com.techstore.dto.response.DashboardTopProductResponse;
import com.techstore.entity.Order;
import com.techstore.entity.OrderItem;
import com.techstore.enums.DashboardPeriod;
import com.techstore.enums.ErrorCode;
import com.techstore.exception.BusinessException;
import com.techstore.repository.OrderRepository;
import com.techstore.service.DashboardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final ZoneOffset REPORT_ZONE = ZoneOffset.UTC;
    private static final int TOP_PRODUCT_LIMIT = 5;
    private static final DateTimeFormatter HOUR_LABEL = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("dd/MM");
    private static final List<String> ORDER_STATUSES = List.of("PENDING", "CONFIRMED", "SHIPPING", "COMPLETED");

    private final OrderRepository orders;

    public DashboardServiceImpl(OrderRepository orders) {
        this.orders = orders;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardResponse getAdminDashboard(String periodValue, LocalDate requestedDate) {
        DashboardPeriod period = parsePeriod(periodValue);
        LocalDate anchorDate = requestedDate == null ? LocalDate.now(REPORT_ZONE) : requestedDate;
        LocalDate fromDate = period == DashboardPeriod.DAY
                ? anchorDate
                : anchorDate.withDayOfMonth(1);
        LocalDate toDate = period == DashboardPeriod.DAY
                ? fromDate.plusDays(1)
                : fromDate.plusMonths(1);

        Instant fromInclusive = fromDate.atStartOfDay().toInstant(REPORT_ZONE);
        Instant toExclusive = toDate.atStartOfDay().toInstant(REPORT_ZONE);
        List<Order> validOrders = orders.findValidOrdersForDashboard(fromInclusive, toExclusive);

        Map<String, Long> statusCounts = new LinkedHashMap<>();
        ORDER_STATUSES.forEach(status -> statusCounts.put(status, 0L));
        Map<String, ProductAccumulator> products = new LinkedHashMap<>();
        Map<String, TrendAccumulator> trend = createTrendBuckets(period, fromDate, toDate);
        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (Order order : validOrders) {
            totalRevenue = totalRevenue.add(nullSafe(order.getTotalAmount()));
            statusCounts.compute(order.getStatus(), (status, count) -> count == null ? 1L : count + 1L);
            String trendKey = trendKey(period, order.getPlacedAt());
            TrendAccumulator trendAccumulator = trend.get(trendKey);
            if (trendAccumulator != null) {
                trendAccumulator.orderCount++;
                trendAccumulator.revenue = trendAccumulator.revenue.add(nullSafe(order.getTotalAmount()));
            }

            for (OrderItem item : order.getItems()) {
                if (item.getProductName() == null || item.getProductName().isBlank()) continue;
                ProductAccumulator product = products.computeIfAbsent(item.getProductName(), ignored -> new ProductAccumulator());
                product.quantitySold += item.getQuantity() == null ? 0 : item.getQuantity();
                BigDecimal itemRevenue = item.getSubtotal();
                if (itemRevenue == null && item.getUnitPrice() != null && item.getQuantity() != null) {
                    itemRevenue = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                }
                product.revenue = product.revenue.add(nullSafe(itemRevenue));
            }
        }

        List<DashboardStatusCountResponse> statusResponses = statusCounts.entrySet().stream()
                .map(entry -> new DashboardStatusCountResponse(entry.getKey(), entry.getValue()))
                .toList();
        List<DashboardTopProductResponse> topProducts = products.entrySet().stream()
                .map(entry -> new DashboardTopProductResponse(entry.getKey(), entry.getValue().quantitySold,
                        money(entry.getValue().revenue)))
                .sorted(Comparator.comparingLong(DashboardTopProductResponse::quantitySold).reversed()
                        .thenComparing(DashboardTopProductResponse::revenue, Comparator.reverseOrder())
                        .thenComparing(DashboardTopProductResponse::productName))
                .limit(TOP_PRODUCT_LIMIT)
                .toList();
        List<DashboardRevenuePointResponse> revenueTrend = trend.values().stream()
                .map(value -> new DashboardRevenuePointResponse(value.label, money(value.revenue), value.orderCount))
                .toList();

        return new AdminDashboardResponse(period, fromDate, toDate.minusDays(1), money(totalRevenue), validOrders.size(),
                statusResponses, topProducts, revenueTrend);
    }

    private DashboardPeriod parsePeriod(String periodValue) {
        if (periodValue == null || periodValue.isBlank()) return DashboardPeriod.MONTH;
        try {
            return DashboardPeriod.valueOf(periodValue.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Khoảng thời gian dashboard chỉ hỗ trợ DAY hoặc MONTH");
        }
    }

    private Map<String, TrendAccumulator> createTrendBuckets(DashboardPeriod period, LocalDate fromDate, LocalDate toDate) {
        Map<String, TrendAccumulator> buckets = new LinkedHashMap<>();
        if (period == DashboardPeriod.DAY) {
            LocalDateTime cursor = fromDate.atStartOfDay();
            for (int hour = 0; hour < 24; hour++) {
                LocalDateTime bucket = cursor.plusHours(hour);
                String key = bucket.toString();
                buckets.put(key, new TrendAccumulator(bucket.format(HOUR_LABEL)));
            }
        } else {
            LocalDate cursor = fromDate;
            while (cursor.isBefore(toDate)) {
                String key = cursor.toString();
                buckets.put(key, new TrendAccumulator(cursor.format(DAY_LABEL)));
                cursor = cursor.plusDays(1);
            }
        }
        return buckets;
    }

    private String trendKey(DashboardPeriod period, Instant placedAt) {
        if (placedAt == null) return "";
        if (period == DashboardPeriod.DAY) {
            LocalDateTime bucket = placedAt.atZone(REPORT_ZONE).toLocalDateTime().withMinute(0).withSecond(0).withNano(0);
            return bucket.toString();
        }
        return placedAt.atZone(REPORT_ZONE).toLocalDate().toString();
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal money(BigDecimal value) {
        return nullSafe(value).setScale(2, RoundingMode.HALF_UP);
    }

    private static final class ProductAccumulator {
        private long quantitySold;
        private BigDecimal revenue = BigDecimal.ZERO;
    }

    private static final class TrendAccumulator {
        private final String label;
        private BigDecimal revenue = BigDecimal.ZERO;
        private long orderCount;

        private TrendAccumulator(String label) {
            this.label = label;
        }
    }
}
