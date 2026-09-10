package com.techstore.repository;

import com.techstore.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByUserId(Long userId, Pageable pageable);

    Page<Order> findByUserIdAndStatus(Long userId, String status, Pageable pageable);

    @Query(value = """
            select o from Order o join fetch o.user u
            where (:search is null
                   or lower(o.orderNumber) like lower(concat('%', :search, '%'))
                   or lower(u.fullName) like lower(concat('%', :search, '%'))
                   or u.phone like concat('%', :search, '%'))
              and (:status is null or o.status = :status)
              and (:fromDate is null or o.placedAt >= :fromDate)
              and (:toDate is null or o.placedAt < :toDate)
            """,
            countQuery = """
                    select count(o) from Order o join o.user u
                    where (:search is null
                           or lower(o.orderNumber) like lower(concat('%', :search, '%'))
                           or lower(u.fullName) like lower(concat('%', :search, '%'))
                           or u.phone like concat('%', :search, '%'))
                      and (:status is null or o.status = :status)
                      and (:fromDate is null or o.placedAt >= :fromDate)
                      and (:toDate is null or o.placedAt < :toDate)
                    """)
    Page<Order> findAdminOrders(
            @Param("search") String search,
            @Param("status") String status,
            @Param("fromDate") java.time.Instant fromDate,
            @Param("toDate") java.time.Instant toDate,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    java.util.Optional<Order> findByIdForUpdate(@Param("id") Long id);

    java.util.Optional<Order> findByOrderNumber(String orderNumber);

    @Query("""
            select distinct o from Order o
            left join fetch o.items
            where o.status <> 'CANCELLED'
              and o.placedAt >= :fromDate
              and o.placedAt < :toDate
            """)
    List<Order> findValidOrdersForDashboard(
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate
    );
}
