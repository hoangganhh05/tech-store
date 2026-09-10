package com.techstore.service.impl;

import com.techstore.dto.request.VoucherRequest;
import com.techstore.dto.response.PageResponse;
import com.techstore.dto.response.VoucherResponse;
import com.techstore.entity.Voucher;
import com.techstore.enums.DiscountType;
import com.techstore.enums.ErrorCode;
import com.techstore.exception.BusinessException;
import com.techstore.repository.VoucherRepository;
import com.techstore.repository.VoucherUsageRepository;
import com.techstore.service.VoucherManagementService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional
public class VoucherManagementServiceImpl implements VoucherManagementService {
    private static final int MAX_PAGE_SIZE = 50;
    private final VoucherRepository vouchers;
    private final VoucherUsageRepository usages;

    public VoucherManagementServiceImpl(VoucherRepository vouchers, VoucherUsageRepository usages) {
        this.vouchers = vouchers;
        this.usages = usages;
    }

    @Override
    public VoucherResponse create(VoucherRequest request) {
        validate(request);
        String code = normalizeCode(request.code());
        if (vouchers.existsByCodeIgnoreCase(code)) {
            throw new BusinessException(ErrorCode.VOUCHER_CODE_DUPLICATE, "Mã voucher đã tồn tại");
        }
        Voucher voucher = new Voucher(code, request.name().trim(), request.discountType(), request.discountValue(),
                request.maxDiscount(), request.minimumOrder(), request.usageLimit(), request.perUserLimit(),
                request.startsAt(), request.endsAt(), request.active() == null || request.active());
        return VoucherResponse.from(vouchers.save(voucher));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<VoucherResponse> getAll(String keyword, int page, int size) {
        validatePage(page, size);
        String search = keyword == null ? "" : keyword.trim();
        Page<Voucher> result = search.isBlank()
                ? vouchers.findAll(PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))))
                : vouchers.findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(search, search,
                PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        return PageResponse.of(result.map(VoucherResponse::from));
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherResponse getById(Long id) {
        return VoucherResponse.from(find(id));
    }

    @Override
    public VoucherResponse update(Long id, VoucherRequest request) {
        validate(request);
        Voucher voucher = find(id);
        String code = normalizeCode(request.code());
        if (vouchers.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new BusinessException(ErrorCode.VOUCHER_CODE_DUPLICATE, "Mã voucher đã tồn tại");
        }
        if (request.usageLimit() != null && request.usageLimit() < voucher.getUsedCount()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Giới hạn lượt dùng không được nhỏ hơn số lượt đã dùng");
        }
        voucher.update(code, request.name(), request.discountType(), request.discountValue(), request.maxDiscount(),
                request.minimumOrder(), request.usageLimit(), request.perUserLimit(), request.startsAt(), request.endsAt(),
                request.active() == null || request.active());
        return VoucherResponse.from(vouchers.save(voucher));
    }

    @Override
    public void delete(Long id) {
        Voucher voucher = find(id);
        if (usages.existsByVoucherId(id)) {
            throw new BusinessException(ErrorCode.VOUCHER_HAS_USAGES,
                    "Không thể xoá voucher đã được sử dụng; hãy chuyển voucher sang trạng thái ngừng hoạt động");
        }
        vouchers.delete(voucher);
    }

    private Voucher find(Long id) {
        if (id == null || id < 1) throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Mã voucher không hợp lệ");
        return vouchers.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.VOUCHER_NOT_FOUND, "Không tìm thấy voucher"));
    }

    private void validate(VoucherRequest request) {
        if (request == null) throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Dữ liệu voucher không hợp lệ");
        if (request.startsAt().isAfter(request.endsAt()) || request.startsAt().equals(request.endsAt())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Thời gian kết thúc phải sau thời gian bắt đầu");
        }
        if (request.discountType() == DiscountType.PERCENT && request.discountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Giá trị giảm theo phần trăm không được vượt quá 100");
        }
        if (request.discountType() == DiscountType.FIXED && request.maxDiscount() != null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Mức giảm tối đa chỉ áp dụng cho voucher phần trăm");
        }
    }

    private String normalizeCode(String code) { return code.trim().toUpperCase(); }
    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Thông tin phân trang không hợp lệ");
        }
    }
}
