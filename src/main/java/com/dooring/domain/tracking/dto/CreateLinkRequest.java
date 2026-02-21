package com.dooring.domain.tracking.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateLinkRequest {

    @NotNull(message = "상품 ID는 필수입니다")
    private Long productId;
}
