package com.dooring.domain.catalog.service;

import com.dooring.common.exception.BusinessException;
import com.dooring.common.exception.ErrorCode;
import com.dooring.domain.catalog.entity.Campaign;
import com.dooring.domain.catalog.repository.CampaignRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository campaignRepository;

    /**
     * 다른 도메인 서비스용 — 현재 시점 활성 캠페인 Entity 반환
     * 활성 캠페인 없으면 예외
     */
    @Transactional(readOnly = true)
    public Campaign findActiveByProduct(Long productId) {
        return campaignRepository.findActiveByProductId(productId)
                .filter(c -> c.isActiveInPeriod(LocalDateTime.now()))
                .orElseThrow(() -> new BusinessException(ErrorCode.CAMPAIGN_NOT_ACTIVE));
    }

    /**
     * 다른 도메인 서비스용 — 현재 시점 활성 캠페인 Optional 반환 (없어도 예외 없음)
     */
    @Transactional(readOnly = true)
    public Optional<Campaign> findActiveByProductOptional(Long productId) {
        return campaignRepository.findActiveByProductId(productId)
                .filter(c -> c.isActiveInPeriod(LocalDateTime.now()));
    }
}
