package com.dooring.domain.order.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 주문 내 개별 상품 (구매 귀속 검증용)
 */
@Entity
@Table(
    name = "order_items",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "order_items_order_product_uniq",
            columnNames = {"order_id", "external_product_id"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 주문 (도메인 내부 참조)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /**
     * 플랫폼 상품 ID (products.external_product_id와 매칭)
     */
    @Column(name = "external_product_id", nullable = false)
    private String externalProductId;

    /**
     * 상품명 스냅샷
     */
    @Column(name = "product_name", columnDefinition = "text")
    private String productName;

    /**
     * 수량
     */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /**
     * 상품별 결제 금액
     */
    @Column(name = "item_amount", precision = 18, scale = 2)
    private BigDecimal itemAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public OrderItem(Order order, String externalProductId, String productName,
                     Integer quantity, BigDecimal itemAmount) {
        this.order = order;
        this.externalProductId = externalProductId;
        this.productName = productName;
        this.quantity = quantity != null ? quantity : 1;
        this.itemAmount = itemAmount;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Order 설정 (양방향 관계 관리용)
     */
    protected void setOrder(Order order) {
        this.order = order;
    }
}
