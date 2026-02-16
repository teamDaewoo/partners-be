package com.dooring.domain.dashboard.querymodel;

import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * 캠페인별 성과 (Value Object)
 * SellerReport의 일부로 사용
 */
@Getter
public class CampaignPerformance {

    private final Long campaignId;
    private final String productName;
    private final BigDecimal commissionAmount;
    private final LocalDateTime startsAt;
    private final LocalDateTime endsAt;
    private final Long totalLinks;
    private final Long totalClicks;
    private final Long totalConversions;
    private final BigDecimal totalCommission;

    public CampaignPerformance(Long campaignId, String productName, BigDecimal commissionAmount,
                              LocalDateTime startsAt, LocalDateTime endsAt,
                              Long totalLinks, Long totalClicks, Long totalConversions,
                              BigDecimal totalCommission) {
        this.campaignId = campaignId;
        this.productName = productName;
        this.commissionAmount = commissionAmount;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.totalLinks = totalLinks != null ? totalLinks : 0L;
        this.totalClicks = totalClicks != null ? totalClicks : 0L;
        this.totalConversions = totalConversions != null ? totalConversions : 0L;
        this.totalCommission = totalCommission != null ? totalCommission : BigDecimal.ZERO;
    }

    /**
     * 전환율 계산 (conversions / clicks)
     *
     * @return 전환율 (0.00 ~ 1.00), 클릭이 없으면 0
     */
    public BigDecimal getConversionRate() {
        if (totalClicks == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(totalConversions)
            .divide(BigDecimal.valueOf(totalClicks), 4, RoundingMode.HALF_UP);
    }

    /**
     * 클릭당 평균 수익 (totalCommission / clicks)
     *
     * @return 클릭당 평균 수익, 클릭이 없으면 0
     */
    public BigDecimal getRevenuePerClick() {
        if (totalClicks == 0) {
            return BigDecimal.ZERO;
        }
        return totalCommission.divide(BigDecimal.valueOf(totalClicks), 2, RoundingMode.HALF_UP);
    }

    /**
     * 전환당 평균 수익 (totalCommission / conversions)
     *
     * @return 전환당 평균 수익, 전환이 없으면 0
     */
    public BigDecimal getRevenuePerConversion() {
        if (totalConversions == 0) {
            return BigDecimal.ZERO;
        }
        return totalCommission.divide(BigDecimal.valueOf(totalConversions), 2, RoundingMode.HALF_UP);
    }

    /**
     * 링크당 평균 클릭 수
     *
     * @return 링크당 평균 클릭 수, 링크가 없으면 0
     */
    public BigDecimal getAverageClicksPerLink() {
        if (totalLinks == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(totalClicks)
            .divide(BigDecimal.valueOf(totalLinks), 2, RoundingMode.HALF_UP);
    }

    /**
     * 캠페인 활성 상태 확인
     *
     * @return 현재 활성 캠페인이면 true
     */
    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(startsAt) && !now.isAfter(endsAt);
    }

    /**
     * ROI 계산 (Return on Investment)
     * 예상 총 매출 대비 커미션 비율
     *
     * @return ROI, 커미션이 0이면 0
     */
    public BigDecimal getEstimatedROI() {
        if (totalCommission.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        // 간단한 ROI: (총 커미션 / 예상 매출액) * 100
        // 실제로는 매출액 데이터가 필요하지만, 여기서는 커미션 비율을 사용
        return commissionAmount
            .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
    }
}
