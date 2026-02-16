package com.dooring.domain.tracking.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 픽셀 전환 이벤트 (idempotent, 웹훅 선/후행 모두 대응)
 * 클라이언트 사이드 픽셀로 수신한 전환 이벤트
 */
@Entity
@Table(
    name = "pixel_events",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "pixel_store_order_uniq",
            columnNames = {"store_id", "external_order_id"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PixelEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 스토어 ID (catalog 도메인 참조)
     */
    @Column(name = "store_id", nullable = false)
    private Long storeId;

    /**
     * 픽셀이 보고한 주문 ID (플랫폼 기준)
     */
    @Column(name = "external_order_id", nullable = false)
    private String externalOrderId;

    /**
     * 어트리뷰션 세션 (도메인 내부 참조, nullable)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribution_session_id")
    private AttributionSession attributionSession;

    /**
     * 이벤트 발생 시각
     */
    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public PixelEvent(Long storeId, String externalOrderId, AttributionSession attributionSession) {
        this.storeId = storeId;
        this.externalOrderId = externalOrderId;
        this.attributionSession = attributionSession;
        this.eventTime = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
    }
}
