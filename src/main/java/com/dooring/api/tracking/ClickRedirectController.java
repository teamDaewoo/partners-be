package com.dooring.api.tracking;

import com.dooring.domain.tracking.dto.ClickRecordResult;
import com.dooring.domain.tracking.service.ClickTrackingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ClickRedirectController {

    private final ClickTrackingService clickTrackingService;

    /**
     * 어필리에이트 링크 클릭 — 클릭 기록 후 상품 페이지로 302 리다이렉트
     * public 엔드포인트 (인증 불필요)
     */
    @GetMapping("/r/{shortCode}")
    public ResponseEntity<Void> redirect(
            @PathVariable String shortCode,
            HttpServletRequest request) {

        String ipAddress = resolveClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        ClickRecordResult result = clickTrackingService.recordClick(shortCode, ipAddress, userAgent);

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, result.redirectUrl())
                .build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
