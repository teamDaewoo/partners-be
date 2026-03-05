package com.dooring.domain.tracking.service;

import com.dooring.domain.tracking.entity.AttributionSession;
import com.dooring.domain.tracking.entity.PixelEvent;
import com.dooring.domain.tracking.repository.AttributionSessionRepository;
import com.dooring.domain.tracking.repository.PixelEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PixelTrackingService {

    private final PixelEventRepository pixelEventRepository;
    private final AttributionSessionRepository attributionSessionRepository;

    /**
     * 픽셀 전환 이벤트 수신 (idempotent)
     * 1. 이미 처리된 주문(storeId + externalOrderId 중복)이면 그냥 반환
     * 2. sessionToken 있으면 만료되지 않은 AttributionSession 조회
     * 3. PixelEvent 저장 (session null이어도 저장 — 미귀속 이벤트)
     */
    @Transactional
    public void recordPixelEvent(Long storeId, String externalOrderId, String sessionToken) {
        if (pixelEventRepository.existsByStoreIdAndExternalOrderId(storeId, externalOrderId)) {
            return;
        }

        Optional<AttributionSession> sessionOpt = Optional.empty();
        if (StringUtils.hasText(sessionToken)) {
            sessionOpt = attributionSessionRepository.findValidSessionByToken(
                    sessionToken, LocalDateTime.now());
        }

        PixelEvent pixelEvent = PixelEvent.builder()
                .storeId(storeId)
                .externalOrderId(externalOrderId)
                .attributionSession(sessionOpt.orElse(null))
                .build();

        pixelEventRepository.save(pixelEvent);
    }
}
