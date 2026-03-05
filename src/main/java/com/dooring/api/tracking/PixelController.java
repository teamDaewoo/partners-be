package com.dooring.api.tracking;

import com.dooring.common.dto.ApiResponse;
import com.dooring.domain.tracking.dto.PixelEventRequest;
import com.dooring.domain.tracking.service.PixelTrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
public class PixelController {

    private final PixelTrackingService pixelTrackingService;

    /**
     * 픽셀 전환 이벤트 수신 (인증 불필요)
     * - 항상 200 반환 (idempotent — 중복 요청 무시)
     * - sessionToken 없으면 미귀속 이벤트로 저장
     */
    @PostMapping("/pixel")
    public ApiResponse<Void> receivePixelEvent(@RequestBody @Valid PixelEventRequest request) {
        pixelTrackingService.recordPixelEvent(
                request.storeId(),
                request.externalOrderId(),
                request.sessionToken()
        );
        return ApiResponse.ok(null);
    }
}
