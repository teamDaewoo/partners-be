package com.dooring.domain.order.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 주문 (플랫폼 웹훅/API 수신, idempotent)
 */
@Entity
@Table(
    name = "orders",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "orders_store_order_uniq",
            columnNames = {"store_id", "external_order_id"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 스토어 ID (catalog 도메인 참조)
     */
    @Column(name = "store_id", nullable = false)
    private Long storeId;

    /**
     * 플랫폼 주문 ID
     */
    @Column(name = "external_order_id", nullable = false)
    private String externalOrderId;

    /**
     * 주문 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    /**
     * 총 결제 금액
     */
    @Column(name = "total_amount", precision = 18, scale = 2)
    private BigDecimal totalAmount;

    /**
     * 실제 주문 시각 (플랫폼 제공)
     */
    @Column(name = "ordered_at")
    private LocalDateTime orderedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 주문 내 상품 목록 (Order가 소유)
     * cascade, orphanRemoval 없음 - Service에서 명시적 관리
     */
    @OneToMany(mappedBy = "order")
    private List<OrderItem> orderItems = new ArrayList<>();

    @Builder
    public Order(Long storeId, String externalOrderId, OrderStatus status,
                 BigDecimal totalAmount, LocalDateTime orderedAt) {
        this.storeId = storeId;
        this.externalOrderId = externalOrderId;
        this.status = status != null ? status : OrderStatus.CREATED;
        this.totalAmount = totalAmount;
        this.orderedAt = orderedAt;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 주문 상태 변경
     */
    public void updateStatus(OrderStatus newStatus) {
        this.status = newStatus;
    }

    /**
     * 주문 아이템 추가
     */
    public void addOrderItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
        orderItem.setOrder(this);
    }

    /**
     * 주문 아이템 제거
     */
    public void removeOrderItem(OrderItem orderItem) {
        this.orderItems.remove(orderItem);
        orderItem.setOrder(null);
    }

    /**
     * 결제 완료 처리
     */
    public void markAsPaid() {
        this.status = OrderStatus.PAID;
    }

    /**
     * 배송 완료 처리
     */
    public void markAsDelivered() {
        this.status = OrderStatus.DELIVERED;
    }

    /**
     * 구매 확정 처리
     */
    public void markAsConfirmed() {
        this.status = OrderStatus.CONFIRMED;
    }

    /**
     * 주문 취소 처리
     */
    public void cancel() {
        this.status = OrderStatus.CANCELLED;
    }

    /**
     * 환불 처리
     */
    public void refund() {
        this.status = OrderStatus.REFUNDED;
    }
}
