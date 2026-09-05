package com.techstore.repository;

import com.techstore.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    /**
     * Lấy tất cả địa chỉ của user, địa chỉ mặc định lên đầu, sau đó sắp theo ngày tạo tăng dần.
     */
    List<Address> findByUserIdOrderByIsDefaultDescCreatedAtAsc(Long userId);

    /**
     * Tìm địa chỉ theo id và userId — đảm bảo ownership.
     */
    Optional<Address> findByIdAndUserId(Long id, Long userId);

    /**
     * Đếm số địa chỉ của user.
     */
    long countByUserId(Long userId);

    /**
     * Tìm địa chỉ mặc định hiện tại của user.
     */
    Optional<Address> findByUserIdAndIsDefaultTrue(Long userId);
}

