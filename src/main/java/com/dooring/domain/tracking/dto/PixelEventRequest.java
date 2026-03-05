package com.dooring.domain.tracking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PixelEventRequest(

        @NotNull(message = "스토어 ID는 필수입니다")
        Long storeId,

        @NotBlank(message = "주문 ID는 필수입니다")
        String externalOrderId,

        String sessionToken  // null 허용 — 세션 없으면 귀속 불가 (미추적 구매)
) {}
