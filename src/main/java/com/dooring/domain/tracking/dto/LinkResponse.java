package com.dooring.domain.tracking.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class LinkResponse {

    private Long linkId;
    private Long productId;
    private String productName;
    private String shortCode;
    private String shortUrl;
    private CampaignInfo campaign;
    private LocalDateTime createdAt;

    @Getter
    @Builder
    public static class CampaignInfo {
        private Long campaignId;
        private BigDecimal commissionAmount;
        private BigDecimal commissionRate;
        private LocalDateTime endsAt;
    }
}
