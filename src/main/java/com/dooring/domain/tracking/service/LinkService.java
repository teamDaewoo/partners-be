package com.dooring.domain.tracking.service;

import com.dooring.common.exception.BusinessException;
import com.dooring.common.exception.ErrorCode;
import com.dooring.common.util.ShortCodeGenerator;
import com.dooring.domain.catalog.entity.Campaign;
import com.dooring.domain.catalog.entity.Product;
import com.dooring.domain.catalog.service.CampaignService;
import com.dooring.domain.catalog.service.ProductService;
import com.dooring.domain.identity.service.CreatorService;
import com.dooring.domain.tracking.dto.LinkResponse;
import com.dooring.domain.tracking.entity.Link;
import com.dooring.domain.tracking.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LinkService {

    private final LinkRepository linkRepository;
    private final CreatorService creatorService;
    private final ProductService productService;
    private final CampaignService campaignService;
    private final ShortCodeGenerator shortCodeGenerator;

    @Value("${dooring.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * 어필리에이트 링크 발급
     * - 크리에이터 × 상품 1:1 멱등: 이미 있으면 기존 링크 반환
     * - 활성 캠페인 없으면 발급 불가
     */
    @Transactional
    public LinkResponse issueLink(Long creatorId, Long productId) {
        creatorService.findEntityById(creatorId);                  // 존재 확인 (없으면 예외)
        Product product = productService.findEntityById(productId); // 존재 확인 (없으면 예외)
        Campaign campaign = campaignService.findActiveByProduct(productId); // 활성 캠페인 확인 (없으면 예외)

        return linkRepository.findByCreatorIdAndProductId(creatorId, productId)
                .map(existing -> toResponse(existing, product, campaign))
                .orElseGet(() -> {
                    Link link = Link.builder()
                            .creatorId(creatorId)
                            .productId(productId)
                            .shortCode(generateUniqueShortCode())
                            .build();
                    return toResponse(linkRepository.save(link), product, campaign);
                });
    }

    /**
     * 크리에이터의 링크 목록 조회
     */
    @Transactional(readOnly = true)
    public List<LinkResponse> getMyLinks(Long creatorId) {
        creatorService.findEntityById(creatorId); // 존재 확인 (없으면 예외)

        return linkRepository.findAllByCreatorId(creatorId).stream()
                .map(link -> {
                    Product product = productService.findEntityById(link.getProductId());
                    Campaign campaign = campaignService.findActiveByProductOptional(link.getProductId())
                            .orElse(null);
                    return toResponse(link, product, campaign);
                })
                .collect(Collectors.toList());
    }

    /** 다른 도메인 서비스용 — shortCode로 Link Entity 반환 */
    @Transactional(readOnly = true)
    public Link findEntityByShortCode(String shortCode) {
        return linkRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.LINK_NOT_FOUND));
    }

    private String generateUniqueShortCode() {
        String code;
        do {
            code = shortCodeGenerator.generate();
        } while (linkRepository.findByShortCode(code).isPresent());
        return code;
    }

    private LinkResponse toResponse(Link link, Product product, Campaign campaign) {
        LinkResponse.CampaignInfo campaignInfo = campaign == null ? null :
                LinkResponse.CampaignInfo.builder()
                        .campaignId(campaign.getId())
                        .commissionAmount(campaign.getCommissionAmount())
                        .commissionRate(campaign.getCommissionRate())
                        .endsAt(campaign.getEndsAt())
                        .build();

        return LinkResponse.builder()
                .linkId(link.getId())
                .productId(product.getId())
                .productName(product.getName())
                .shortCode(link.getShortCode())
                .shortUrl(baseUrl + "/r/" + link.getShortCode())
                .campaign(campaignInfo)
                .createdAt(link.getCreatedAt())
                .build();
    }
}
