package com.dooring.domain.dashboard.querymodel;

import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 링크별 성과 (Value Object)
 * CreatorReport의 일부로 사용
 */
@Getter
public class LinkPerformance {

    private final Long linkId;
    private final String shortCode;
    private final String productName;
    private final Long clicks;
    private final Long conversions;
    private final BigDecimal totalCommission;

    public LinkPerformance(Long linkId, String shortCode, String productName,
                          Long clicks, Long conversions, BigDecimal totalCommission) {
        this.linkId = linkId;
        this.shortCode = shortCode;
        this.productName = productName;
        this.clicks = clicks != null ? clicks : 0L;
        this.conversions = conversions != null ? conversions : 0L;
        this.totalCommission = totalCommission != null ? totalCommission : BigDecimal.ZERO;
    }

    /**
     * 전환율 계산 (conversions / clicks)
     *
     * @return 전환율 (0.00 ~ 1.00), 클릭이 없으면 0
     */
    public BigDecimal getConversionRate() {
        if (clicks == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(conversions)
            .divide(BigDecimal.valueOf(clicks), 4, RoundingMode.HALF_UP);
    }

    /**
     * 클릭당 평균 수익 (totalCommission / clicks)
     *
     * @return 클릭당 평균 수익, 클릭이 없으면 0
     */
    public BigDecimal getRevenuePerClick() {
        if (clicks == 0) {
            return BigDecimal.ZERO;
        }
        return totalCommission.divide(BigDecimal.valueOf(clicks), 2, RoundingMode.HALF_UP);
    }

    /**
     * 전환당 평균 수익 (totalCommission / conversions)
     *
     * @return 전환당 평균 수익, 전환이 없으면 0
     */
    public BigDecimal getRevenuePerConversion() {
        if (conversions == 0) {
            return BigDecimal.ZERO;
        }
        return totalCommission.divide(BigDecimal.valueOf(conversions), 2, RoundingMode.HALF_UP);
    }
}
