package com.dooring.api.creator;

import com.dooring.common.dto.ApiResponse;
import com.dooring.domain.tracking.dto.CreateLinkRequest;
import com.dooring.domain.tracking.dto.LinkResponse;
import com.dooring.domain.tracking.service.LinkService;
import com.dooring.infrastructure.security.CreatorPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/creator")
@RequiredArgsConstructor
public class LinkController {

    private final LinkService linkService;

    /** 어필리에이트 링크 발급 */
    @PostMapping("/links")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LinkResponse> issueLink(
            @AuthenticationPrincipal CreatorPrincipal principal,
            @RequestBody @Valid CreateLinkRequest request
    ) {
        return ApiResponse.ok(linkService.issueLink(principal.getId(), request.getProductId()));
    }

    /** 내 링크 목록 조회 */
    @GetMapping("/links")
    public ApiResponse<List<LinkResponse>> getMyLinks(
            @AuthenticationPrincipal CreatorPrincipal principal
    ) {
        return ApiResponse.ok(linkService.getMyLinks(principal.getId()));
    }
}
