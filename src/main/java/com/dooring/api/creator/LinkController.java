package com.dooring.api.creator;

import com.dooring.common.dto.ApiResponse;
import com.dooring.domain.tracking.dto.CreateLinkRequest;
import com.dooring.domain.tracking.dto.LinkResponse;
import com.dooring.domain.tracking.service.LinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/creator")
@RequiredArgsConstructor
public class LinkController {

    private final LinkService linkService;

    /**
     * 어필리에이트 링크 발급
     * TODO: auth 구현 후 @RequestParam creatorId → @AuthenticationPrincipal 로 교체
     */
    @PostMapping("/links")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LinkResponse> issueLink(
            @RequestParam Long creatorId,
            @RequestBody @Valid CreateLinkRequest request
    ) {
        return ApiResponse.ok(linkService.issueLink(creatorId, request.getProductId()));
    }

    /**
     * 내 링크 목록 조회
     * TODO: auth 구현 후 @RequestParam creatorId → @AuthenticationPrincipal 로 교체
     */
    @GetMapping("/links")
    public ApiResponse<List<LinkResponse>> getMyLinks(
            @RequestParam Long creatorId
    ) {
        return ApiResponse.ok(linkService.getMyLinks(creatorId));
    }
}
