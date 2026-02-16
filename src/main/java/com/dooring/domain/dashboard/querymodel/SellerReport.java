package com.dooring.domain.dashboard.querymodel;

import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 셀러 지급 현황 리포트 (QueryModel)
 * 여러 테이블의 데이터를 집계한 비즈니스 집계 모델
 */
@Getter
public class SellerReport {

    private final Long sellerId;
    private final String sellerName;
    private final Long totalCampaigns;
    private final Long totalConversions;
    private final BigDecimal pendingCommission;
    private final BigDecimal confirmedCommission;
    private final BigDecimal paidCommission;
    private final List<CampaignPerformance> campaignPerformances;

    public SellerReport(Long sellerId, String sellerName,
                       Long totalCampaigns, Long totalConversions,
                       BigDecimal pendingCommission, BigDecimal confirmedCommission,
                       BigDecimal paidCommission, List<CampaignPerformance> campaignPerformances) {
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.totalCampaigns = totalCampaigns != null ? totalCampaigns : 0L;
        this.totalConversions = totalConversions != null ? totalConversions : 0L;
        this.pendingCommission = pendingCommission != null ? pendingCommission : BigDecimal.ZERO;
        this.confirmedCommission = confirmedCommission != null ? confirmedCommission : BigDecimal.ZERO;
        this.paidCommission = paidCommission != null ? paidCommission : BigDecimal.ZERO;
        this.campaignPerformances = campaignPerformances != null ? campaignPerformances : List.of();
    }

    /**
     * 전체 커미션 합계 (모든 상태)
     *
     * @return 전체 커미션 합계
     */
    public BigDecimal getTotalCommission() {
        return pendingCommission.add(confirmedCommission).add(paidCommission);
    }

    /**
     * 미지급 커미션 (PENDING + CONFIRMED)
     * 아직 셀러가 지급하지 않은 커미션
     *
     * @return 미지급 커미션
     */
    public BigDecimal getUnpaidCommission() {
        return pendingCommission.add(confirmedCommission);
    }

    /**
     * 캠페인당 평균 전환 수
     *
     * @return 캠페인당 평균 전환 수, 캠페인이 없으면 0
     */
    public BigDecimal getAverageConversionsPerCampaign() {
        if (totalCampaigns == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(totalConversions)
            .divide(BigDecimal.valueOf(totalCampaigns), 2, RoundingMode.HALF_UP);
    }

    /**
     * 캠페인당 평균 커미션
     *
     * @return 캠페인당 평균 커미션, 캠페인이 없으면 0
     */
    public BigDecimal getAverageCommissionPerCampaign() {
        if (totalCampaigns == 0) {
            return BigDecimal.ZERO;
        }
        return getTotalCommission().divide(BigDecimal.valueOf(totalCampaigns), 2, RoundingMode.HALF_UP);
    }

    /**
     * 전환당 평균 커미션
     *
     * @return 전환당 평균 커미션, 전환이 없으면 0
     */
    public BigDecimal getAverageCommissionPerConversion() {
        if (totalConversions == 0) {
            return BigDecimal.ZERO;
        }
        return getTotalCommission().divide(BigDecimal.valueOf(totalConversions), 2, RoundingMode.HALF_UP);
    }

    /**
     * 커미션 지급률 (paid / total)
     * 전체 커미션 중 얼마나 지급했는지 비율
     *
     * @return 지급률 (0.00 ~ 1.00), 총 커미션이 0이면 0
     */
    public BigDecimal getPaymentRate() {
        BigDecimal total = getTotalCommission();
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return paidCommission.divide(total, 4, RoundingMode.HALF_UP);
    }

    /**
     * 활성 캠페인 수
     *
     * @return 현재 활성화된 캠페인 수
     */
    public long getActiveCampaignsCount() {
        return campaignPerformances.stream()
            .filter(CampaignPerformance::isActive)
            .count();
    }
}
