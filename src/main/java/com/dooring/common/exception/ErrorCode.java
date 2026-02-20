package com.dooring.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_INPUT_VALUE(400, "잘못된 입력값입니다"),

    // Identity
    CREATOR_NOT_FOUND(404, "크리에이터를 찾을 수 없습니다"),
    SELLER_NOT_FOUND(404, "셀러를 찾을 수 없습니다"),

    // Catalog
    PRODUCT_NOT_FOUND(404, "상품을 찾을 수 없습니다"),
    CAMPAIGN_NOT_ACTIVE(400, "현재 운영 중인 캠페인이 없습니다. 링크를 발급하려면 활성 캠페인이 필요합니다"),

    // Tracking
    LINK_NOT_FOUND(404, "링크를 찾을 수 없습니다"),

    // Attribution
    DUPLICATE_ATTRIBUTION(409, "이미 처리된 주문입니다"),
    ATTRIBUTION_WINDOW_EXPIRED(400, "어트리뷰션 윈도우가 만료되었습니다");

    private final int status;
    private final String message;
}
