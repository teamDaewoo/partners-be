package com.dooring.domain.dashboard.querymodel;

import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 크리에이터 실적 리포트 (QueryModel)
 * 여러 테이블의 데이터를 집계한 비즈니스 집계 모델
 */
@Getter
public class CreatorReport {

    private final Long creatorId;
    private final String nickname;
    private final Long totalLinks;
    private final Long totalClicks;
    private final Long totalConversions;
    private final BigDecimal pendingCommission;
    private final BigDecimal confirmedCommission;
    private final BigDecimal paidCommission;
    private final List<LinkPerformance> linkPerformances;

    public CreatorReport(Long creatorId, String nickname,
                        Long totalLinks, Long totalClicks, Long totalConversions,
                        BigDecimal pendingCommission, BigDecimal confirmedCommission,
                        BigDecimal paidCommission, List<LinkPerformance> linkPerformances) {
        this.creatorId = creatorId;
        this.nickname = nickname;
        this.totalLinks = totalLinks != null ? totalLinks : 0L;
        this.totalClicks = totalClicks != null ? totalClicks : 0L;
        this.totalConversions = totalConversions != null ? totalConversions : 0L;
        this.pendingCommission = pendingCommission != null ? pendingCommission : BigDecimal.ZERO;
        this.confirmedCommission = confirmedCommission != null ? confirmedCommission : BigDecimal.ZERO;
        this.paidCommission = paidCommission != null ? paidCommission : BigDecimal.ZERO;
        this.linkPerformances = linkPerformances != null ? linkPerformances : List.of();
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
        return getTotalCommission().divide(BigDecimal.valueOf(totalClicks), 2, RoundingMode.HALF_UP);
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
        return getTotalCommission().divide(BigDecimal.valueOf(totalConversions), 2, RoundingMode.HALF_UP);
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
     * 링크당 평균 전환 수
     *
     * @return 링크당 평균 전환 수, 링크가 없으면 0
     */
    public BigDecimal getAverageConversionsPerLink() {
        if (totalLinks == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(totalConversions)
            .divide(BigDecimal.valueOf(totalLinks), 2, RoundingMode.HALF_UP);
    }
}
