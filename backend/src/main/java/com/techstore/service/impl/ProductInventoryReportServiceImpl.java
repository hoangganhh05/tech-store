package com.techstore.service.impl;

import com.techstore.dto.response.LowStockReportItem;
import com.techstore.dto.response.ProductInventoryReportResponse;
import com.techstore.dto.response.ProductSalesReportItem;
import com.techstore.entity.Inventory;
import com.techstore.entity.Order;
import com.techstore.entity.OrderItem;
import com.techstore.entity.ProductVariant;
import com.techstore.enums.ErrorCode;
import com.techstore.exception.BusinessException;
import com.techstore.repository.InventoryRepository;
import com.techstore.repository.OrderRepository;
import com.techstore.repository.ProductVariantRepository;
import com.techstore.service.ProductInventoryReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ProductInventoryReportServiceImpl implements ProductInventoryReportService {

    private static final ZoneOffset REPORT_ZONE = ZoneOffset.UTC;
    private static final int TOP_PRODUCT_LIMIT = 10;

    private final OrderRepository orders;
    private final ProductVariantRepository variants;
    private final InventoryRepository inventories;

    public ProductInventoryReportServiceImpl(OrderRepository orders,
                                             ProductVariantRepository variants,
                                             InventoryRepository inventories) {
        this.orders = orders;
        this.variants = variants;
        this.inventories = inventories;
    }

    @Override
    @Transactional(readOnly = true)
    public ProductInventoryReportResponse getReport(LocalDate fromDate, LocalDate toDate,
                                                    Long categoryId, String sortBy, String sortDirection) {
        validateRange(fromDate, toDate);
        validateCategory(categoryId);
        SalesSort salesSort = parseSort(sortBy);
        Direction direction = parseDirection(sortDirection);

        Instant fromInclusive = fromDate.atStartOfDay().toInstant(REPORT_ZONE);
        Instant toExclusive = toDate.plusDays(1).atStartOfDay().toInstant(REPORT_ZONE);
        List<Order> validOrders = orders.findValidOrdersForDashboard(fromInclusive, toExclusive);

        Map<Long, ProductVariant> variantById = loadVariantsForSales(validOrders);
        Map<ProductKey, SalesAccumulator> sales = aggregateSales(validOrders, categoryId, variantById);
        Comparator<ProductSalesReportItem> comparator = comparatorFor(salesSort, direction);
        List<ProductSalesReportItem> topSellingProducts = sales.entrySet().stream()
                .filter(entry -> entry.getValue().quantitySold > 0)
                .map(entry -> new ProductSalesReportItem(
                        entry.getKey().productName,
                        entry.getKey().categoryId,
                        entry.getKey().categoryName,
                        entry.getValue().quantitySold,
                        money(entry.getValue().revenue)
                ))
                .sorted(comparator)
                .limit(TOP_PRODUCT_LIMIT)
                .toList();

        List<LowStockReportItem> lowStockVariants = inventories.findLowStockForReport(categoryId).stream()
                .map(this::toLowStockItem)
                .toList();

        return new ProductInventoryReportResponse(
                fromDate,
                toDate,
                categoryId,
                salesSort.name(),
                direction.name(),
                topSellingProducts,
                lowStockVariants
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

    private void validateCategory(Long categoryId) {
        if (categoryId != null && categoryId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Danh mục không hợp lệ");
        }
    }

    private SalesSort parseSort(String value) {
        if (value == null || value.isBlank()) return SalesSort.QUANTITY;
        try {
            return SalesSort.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Tiêu chí sắp xếp chỉ hỗ trợ QUANTITY, REVENUE hoặc NAME");
        }
    }

    private Direction parseDirection(String value) {
        if (value == null || value.isBlank()) return Direction.DESC;
        try {
            return Direction.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Chiều sắp xếp chỉ hỗ trợ ASC hoặc DESC");
        }
    }

    private Map<Long, ProductVariant> loadVariantsForSales(List<Order> validOrders) {
        Set<Long> variantIds = new HashSet<>();
        validOrders.forEach(order -> order.getItems().forEach(item -> {
            if (item.getVariantId() != null) variantIds.add(item.getVariantId());
        }));
        if (variantIds.isEmpty()) return Map.of();
        Map<Long, ProductVariant> result = new HashMap<>();
        variants.findAllById(variantIds).forEach(variant -> result.put(variant.getId(), variant));
        return result;
    }

    private Map<ProductKey, SalesAccumulator> aggregateSales(List<Order> validOrders, Long categoryId,
                                                               Map<Long, ProductVariant> variantById) {
        Map<ProductKey, SalesAccumulator> sales = new LinkedHashMap<>();
        for (Order order : validOrders) {
            for (OrderItem item : order.getItems()) {
                String productName = item.getProductName();
                int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
                if (productName == null || productName.isBlank() || quantity <= 0) continue;

                ProductVariant variant = variantById.get(item.getVariantId());
                Long itemCategoryId = variant != null && variant.getProduct() != null
                        && variant.getProduct().getCategory() != null
                        ? variant.getProduct().getCategory().getId() : null;
                if (categoryId != null && !categoryId.equals(itemCategoryId)) continue;
                String categoryName = variant != null && variant.getProduct() != null
                        && variant.getProduct().getCategory() != null
                        ? variant.getProduct().getCategory().getName() : null;

                ProductKey key = new ProductKey(productName.trim(), itemCategoryId, categoryName);
                SalesAccumulator accumulator = sales.computeIfAbsent(key, ignored -> new SalesAccumulator());
                accumulator.quantitySold += quantity;
                BigDecimal subtotal = item.getSubtotal();
                if (subtotal == null && item.getUnitPrice() != null) {
                    subtotal = item.getUnitPrice().multiply(BigDecimal.valueOf(quantity));
                }
                accumulator.revenue = accumulator.revenue.add(nullSafe(subtotal));
            }
        }
        return sales;
    }

    private Comparator<ProductSalesReportItem> comparatorFor(SalesSort sort, Direction direction) {
        Comparator<ProductSalesReportItem> comparator = switch (sort) {
            case QUANTITY -> Comparator.comparingLong(ProductSalesReportItem::quantitySold)
                    .thenComparing(ProductSalesReportItem::revenue, Comparator.reverseOrder());
            case REVENUE -> Comparator.comparing(ProductSalesReportItem::revenue)
                    .thenComparingLong(ProductSalesReportItem::quantitySold);
            case NAME -> Comparator.comparing(ProductSalesReportItem::productName, String.CASE_INSENSITIVE_ORDER);
        };
        Comparator<ProductSalesReportItem> ordered = direction == Direction.DESC ? comparator.reversed() : comparator;
        return ordered.thenComparing(ProductSalesReportItem::productName, String.CASE_INSENSITIVE_ORDER);
    }

    private LowStockReportItem toLowStockItem(Inventory inventory) {
        ProductVariant variant = inventory.getVariant();
        var product = variant == null ? null : variant.getProduct();
        var category = product == null ? null : product.getCategory();
        return new LowStockReportItem(
                variant == null ? null : variant.getId(),
                product == null ? null : product.getId(),
                product == null ? null : product.getName(),
                category == null ? null : category.getId(),
                category == null ? null : category.getName(),
                variant == null ? null : variant.getSku(),
                variant == null ? null : variant.getColor(),
                variant == null ? null : variant.getStorage(),
                inventory.getQuantityOnHand(),
                inventory.getQuantityReserved(),
                inventory.getAvailableQuantity(),
                inventory.getLowStockThreshold(),
                inventory.getStockStatus().name()
        );
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal money(BigDecimal value) {
        return nullSafe(value).setScale(2, RoundingMode.HALF_UP);
    }

    private enum SalesSort { QUANTITY, REVENUE, NAME }

    private enum Direction { ASC, DESC }

    private record ProductKey(String productName, Long categoryId, String categoryName) { }

    private static final class SalesAccumulator {
        private long quantitySold;
        private BigDecimal revenue = BigDecimal.ZERO;
    }
}
