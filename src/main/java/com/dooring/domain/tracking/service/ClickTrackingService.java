package com.dooring.domain.tracking.service;

import com.dooring.common.exception.BusinessException;
import com.dooring.common.exception.ErrorCode;
import com.dooring.domain.catalog.entity.Campaign;
import com.dooring.domain.catalog.entity.Product;
import com.dooring.domain.catalog.service.CampaignService;
import com.dooring.domain.catalog.service.ProductService;
import com.dooring.domain.tracking.dto.ClickRecordResult;
import com.dooring.domain.tracking.entity.AttributionSession;
import com.dooring.domain.tracking.entity.Click;
import com.dooring.domain.tracking.entity.Link;
import com.dooring.domain.tracking.repository.AttributionSessionRepository;
import com.dooring.domain.tracking.repository.ClickRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClickTrackingService {

    private final LinkService linkService;
    private final ProductService productService;
    private final CampaignService campaignService;
    private final ClickRepository clickRepository;
    private final AttributionSessionRepository attributionSessionRepository;

    /**
     * 클릭 기록 + AttributionSession 생성
     * 1. shortCode → Link 조회
     * 2. Link → Product 조회 (productUrl 검증)
     * 3. 활성 캠페인 스냅샷 추출 (없으면 null — 비활성 기간 클릭도 저장)
     * 4. Click 저장
     * 5. AttributionSession 저장 (sessionToken = clickToken, TTL = 24h)
     * 6. redirectUrl = productUrl + "?dooring_session=" + sessionToken
     */
    @Transactional
    public ClickRecordResult recordClick(String shortCode, String ipAddress, String userAgent) {
        Link link = linkService.findEntityByShortCode(shortCode);

        Product product = productService.findEntityById(link.getProductId());

        String productUrl = product.getProductUrl();
        if (productUrl == null || productUrl.isBlank()) {
            throw new BusinessException(ErrorCode.PRODUCT_URL_NOT_FOUND);
        }

        Optional<Campaign> campaignOpt = campaignService.findActiveByProductOptional(link.getProductId());

        String clickToken = UUID.randomUUID().toString();

        Click click = Click.builder()
                .link(link)
                .campaignId(campaignOpt.map(Campaign::getId).orElse(null))
                .commissionSnapshotAmount(campaignOpt.map(Campaign::getCommissionAmount).orElse(null))
                .commissionSnapshotRate(campaignOpt.map(Campaign::getCommissionRate).orElse(null))
                .clickToken(clickToken)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        clickRepository.save(click);

        AttributionSession session = AttributionSession.builder()
                .sessionToken(clickToken)
                .click(click)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        attributionSessionRepository.save(session);

        String finalRedirectUrl = productUrl + "?dooring_session=" + session.getSessionToken();

        return new ClickRecordResult(finalRedirectUrl, session.getSessionToken());
    }
}
