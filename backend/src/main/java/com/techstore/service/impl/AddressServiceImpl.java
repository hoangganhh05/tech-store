package com.techstore.service.impl;

import com.techstore.dto.request.AddressRequest;
import com.techstore.dto.response.AddressResponse;
import com.techstore.entity.Address;
import com.techstore.entity.User;
import com.techstore.enums.ErrorCode;
import com.techstore.enums.UserStatus;
import com.techstore.exception.BusinessException;
import com.techstore.repository.AddressRepository;
import com.techstore.repository.UserRepository;
import com.techstore.service.AddressService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public AddressServiceImpl(AddressRepository addressRepository, UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> listAddresses(Long userId) {
        findAuthorizedUser(userId);
        return addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtAsc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public AddressResponse addAddress(Long userId, AddressRequest request) {
        User user = findAuthorizedUser(userId);

        Address address = new Address(
                user,
                request.getRecipientName().trim(),
                request.getPhone().trim(),
                request.getProvince().trim(),
                request.getDistrict().trim(),
                request.getWard().trim(),
                request.getStreetAddress().trim()
        );

        // Địa chỉ đầu tiên tự động là mặc định
        boolean isFirstAddress = addressRepository.countByUserId(userId) == 0;
        if (isFirstAddress) {
            address.markAsDefault();
        }

        return toResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request) {
        findAuthorizedUser(userId);
        Address address = findOwnedAddress(userId, addressId);

        address.update(
                request.getRecipientName().trim(),
                request.getPhone().trim(),
                request.getProvince().trim(),
                request.getDistrict().trim(),
                request.getWard().trim(),
                request.getStreetAddress().trim()
        );

        return toResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        findAuthorizedUser(userId);
        Address address = findOwnedAddress(userId, addressId);

        // Không cho xoá nếu là địa chỉ duy nhất và đang là mặc định
        if (address.isDefault() && addressRepository.countByUserId(userId) == 1) {
            throw new BusinessException(
                    ErrorCode.ADDRESS_CANNOT_DELETE_ONLY_DEFAULT,
                    "Không thể xoá địa chỉ duy nhất đang được đặt làm mặc định. Vui lòng thêm địa chỉ khác trước."
            );
        }

        addressRepository.delete(address);

        // Nếu xoá địa chỉ mặc định và vẫn còn địa chỉ khác → tự động đặt địa chỉ cũ nhất làm mặc định
        if (address.isDefault()) {
            List<Address> remaining = addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtAsc(userId);
            if (!remaining.isEmpty()) {
                Address oldest = remaining.get(remaining.size() - 1);
                oldest.markAsDefault();
                addressRepository.save(oldest);
            }
        }
    }

    @Override
    @Transactional
    public AddressResponse setDefaultAddress(Long userId, Long addressId) {
        findAuthorizedUser(userId);
        Address newDefault = findOwnedAddress(userId, addressId);

        if (newDefault.isDefault()) {
            // Đã là mặc định rồi — trả về ngay không cần làm gì
            return toResponse(newDefault);
        }

        // Bỏ mặc định cũ
        addressRepository.findByUserIdAndIsDefaultTrue(userId).ifPresent(old -> {
            old.unmarkAsDefault();
            addressRepository.save(old);
        });

        // Đặt mặc định mới
        newDefault.markAsDefault();
        return toResponse(addressRepository.save(newDefault));
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private User findAuthorizedUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INVALID_ACCESS_TOKEN,
                        "Phiên đăng nhập không hợp lệ hoặc đã hết hạn"
                ));
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED, "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ hỗ trợ.");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED, "Tài khoản hiện không thể sử dụng. Vui lòng liên hệ hỗ trợ.");
        }
        return user;
    }

    private Address findOwnedAddress(Long userId, Long addressId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ADDRESS_NOT_FOUND,
                        "Địa chỉ không tồn tại hoặc bạn không có quyền truy cập"
                ));
    }

    private AddressResponse toResponse(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getRecipientName(),
                address.getPhone(),
                address.getProvince(),
                address.getDistrict(),
                address.getWard(),
                address.getStreetAddress(),
                address.isDefault(),
                address.getCreatedAt()
        );
    }
}

