# api 패키지

## 역할
**HTTP 바인딩만 담당.** Controller만 존재하며, 비즈니스 로직은 절대 포함하지 않음.

## 책임
- HTTP 요청 수신 및 응답 반환
- 요청 데이터 검증 (`@Valid`)
- 인증 정보 추출 (`@AuthenticationPrincipal`)
- 도메인 서비스 호출
- 공통 응답 래핑 (`ApiResponse`)

## 금지 사항
- ❌ `@Transactional` 사용 금지
- ❌ 비즈니스 로직 작성 금지
- ❌ Repository 직접 호출 금지
- ❌ DTO 변환 금지 (서비스에서 이미 변환된 DTO를 받음)

## 구조
```
api/
├── seller/          # 판매자 관련 Controller
├── creator/         # 크리에이터 관련 Controller
├── tracking/        # 트래킹 관련 Controller
├── webhook/         # 웹훅 수신 Controller
└── dashboard/       # 대시보드 조회 Controller
```

## 코드 예시
```java
@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;

    @PostMapping
    public ApiResponse<CampaignResponse> create(
            @AuthenticationPrincipal SellerPrincipal seller,
            @RequestBody @Valid CreateCampaignRequest req) {
        return ApiResponse.ok(campaignService.create(seller.getId(), req));
    }

    @GetMapping("/{id}")
    public ApiResponse<CampaignResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(campaignService.getById(id));
    }
}
```

## 원칙
- Controller는 **얇게** 유지
- 모든 비즈니스 로직은 **도메인 서비스**로 위임
- 서비스에서 반환된 DTO를 **그대로** 응답에 사용
