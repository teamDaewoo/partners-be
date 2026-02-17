package com.dooring.domain.order.repository;

import com.dooring.domain.order.entity.Order;
import com.dooring.domain.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 주문 Repository
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 스토어와 외부 주문 ID로 조회 (idempotent)
     *
     * @param storeId 스토어 ID
     * @param externalOrderId 외부 주문 ID
     * @return 주문
     */
    Optional<Order> findByStoreIdAndExternalOrderId(Long storeId, String externalOrderId);

    /**
     * 스토어와 외부 주문 ID 존재 여부 확인
     *
     * @param storeId 스토어 ID
     * @param externalOrderId 외부 주문 ID
     * @return 존재 여부
     */
    boolean existsByStoreIdAndExternalOrderId(Long storeId, String externalOrderId);

    /**
     * 스토어의 모든 주문 조회
     *
     * @param storeId 스토어 ID
     * @return 주문 목록
     */
    List<Order> findAllByStoreId(Long storeId);

    /**
     * 스토어의 특정 상태 주문 조회
     *
     * @param storeId 스토어 ID
     * @param status 주문 상태
     * @return 주문 목록
     */
    List<Order> findAllByStoreIdAndStatus(Long storeId, OrderStatus status);

    /**
     * 특정 상태의 주문 조회
     *
     * @param status 주문 상태
     * @return 주문 목록
     */
    List<Order> findAllByStatus(OrderStatus status);

    /**
     * 특정 기간 내 주문 조회
     *
     * @param storeId 스토어 ID
     * @param startTime 시작 시각
     * @param endTime 종료 시각
     * @return 주문 목록
     */
    @Query("SELECT o FROM Order o WHERE o.storeId = :storeId " +
           "AND o.orderedAt >= :startTime AND o.orderedAt <= :endTime")
    List<Order> findOrdersInPeriod(
        @Param("storeId") Long storeId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * 특정 기간 내 특정 상태의 주문 조회
     *
     * @param storeId 스토어 ID
     * @param status 주문 상태
     * @param startTime 시작 시각
     * @param endTime 종료 시각
     * @return 주문 목록
     */
    @Query("SELECT o FROM Order o WHERE o.storeId = :storeId " +
           "AND o.status = :status " +
           "AND o.orderedAt >= :startTime AND o.orderedAt <= :endTime")
    List<Order> findOrdersByStatusInPeriod(
        @Param("storeId") Long storeId,
        @Param("status") OrderStatus status,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * OrderItem을 fetch join하여 주문 조회
     *
     * @param orderId 주문 ID
     * @return 주문
     */
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.id = :orderId")
    Optional<Order> findByIdWithItems(@Param("orderId") Long orderId);
}
