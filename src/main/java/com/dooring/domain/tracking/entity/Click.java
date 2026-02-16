package com.dooring.domain.tracking.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 클릭 로그 (캠페인 스냅샷 포함, 어트리뷰션 윈도우 24h)
 * 링크 클릭 시점의 캠페인 정보를 스냅샷으로 저장
 */
@Entity
@Table(
    name = "clicks",
    indexes = {
        @Index(name = "clicks_link_clicked_at_idx", columnList = "link_id, clicked_at"),
        @Index(name = "clicks_token_idx", columnList = "click_token")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Click {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 링크 (도메인 내부 참조)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "link_id", nullable = false)
    private Link link;

    /**
     * 클릭 시점 활성 캠페인 ID (catalog 도메인 참조)
     * NULL이면 비활성 기간 클릭
     */
    @Column(name = "campaign_id")
    private Long campaignId;

    /**
     * 클릭 시점 커미션 금액 (스냅샷)
     */
    @Column(name = "commission_snapshot_amount", precision = 18, scale = 2)
    private BigDecimal commissionSnapshotAmount;

    /**
     * 클릭 시점 커미션 비율 (스냅샷)
     */
    @Column(name = "commission_snapshot_rate", precision = 5, scale = 4)
    private BigDecimal commissionSnapshotRate;

    /**
     * URL 파라미터 + 쿠키 + 세션 스토리지에 저장되는 추적 토큰
     */
    @Column(name = "click_token", nullable = false, unique = true)
    private String clickToken;

    /**
     * IP 주소 (중복 클릭 필터링용)
     */
    @Column(name = "ip_address", columnDefinition = "inet")
    private String ipAddress;

    /**
     * User-Agent
     */
    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;

    /**
     * 클릭 발생 시각
     */
    @Column(name = "clicked_at", nullable = false)
    private LocalDateTime clickedAt;

    @Builder
    public Click(Link link, Long campaignId, BigDecimal commissionSnapshotAmount,
                 BigDecimal commissionSnapshotRate, String clickToken,
                 String ipAddress, String userAgent) {
        this.link = link;
        this.campaignId = campaignId;
        this.commissionSnapshotAmount = commissionSnapshotAmount;
        this.commissionSnapshotRate = commissionSnapshotRate;
        this.clickToken = clickToken;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.clickedAt = LocalDateTime.now();
    }
}
