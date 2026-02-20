# Tracking Service — 1단계: 어필리에이트 링크 발급

## 개요

크리에이터가 상품에 대한 어필리에이트 링크를 발급받는 기능입니다.
발급된 단축 링크는 SNS/블로그에 공유되며, 링크를 통한 구매 발생 시 커미션이 지급됩니다.

---

## 구현 파일

```
common/
├── dto/ApiResponse.java
├── exception/ErrorCode.java
├── exception/BusinessException.java
├── exception/GlobalExceptionHandler.java
└── util/ShortCodeGenerator.java

domain/
├── identity/service/
│   └── CreatorService.java           ← findEntityById()
├── catalog/service/
│   ├── ProductService.java           ← findEntityById()
│   └── CampaignService.java          ← findActiveByProduct(), findActiveByProductOptional()
└── tracking/
    ├── dto/
    │   ├── CreateLinkRequest.java
    │   └── LinkResponse.java
    └── service/
        └── LinkService.java          ← issueLink(), getMyLinks(), findEntityByShortCode()

api/creator/
└── LinkController.java
```

---

## API

### 링크 발급

```
POST /api/creator/links?creatorId={creatorId}
```

**Request**
```json
{
  "productId": 42
}
```

**Response 201**
```json
{
  "success": true,
  "data": {
    "linkId": 1,
    "productId": 42,
    "productName": "나이키 에어맥스 270",
    "shortCode": "aB3kX9Zw",
    "shortUrl": "http://localhost:8080/r/aB3kX9Zw",
    "campaign": {
      "campaignId": 7,
      "commissionAmount": 3000.00,
      "commissionRate": 0.0300,
      "endsAt": "2024-12-31T23:59:59"
    },
    "createdAt": "2024-02-20T10:00:00"
  }
}
```

**에러 케이스**

| HTTP | ErrorCode            | 조건                          |
|------|----------------------|-------------------------------|
| 404  | CREATOR_NOT_FOUND    | creatorId 존재하지 않음       |
| 404  | PRODUCT_NOT_FOUND    | productId 존재하지 않음       |
| 400  | CAMPAIGN_NOT_ACTIVE  | 활성 캠페인 없음              |

---

### 링크 목록 조회

```
GET /api/creator/links?creatorId={creatorId}
```

**Response 200**
```json
{
  "success": true,
  "data": [
    {
      "linkId": 1,
      "productId": 42,
      "productName": "나이키 에어맥스 270",
      "shortCode": "aB3kX9Zw",
      "shortUrl": "http://localhost:8080/r/aB3kX9Zw",
      "campaign": { ... },
      "createdAt": "2024-02-20T10:00:00"
    }
  ]
}
```

> 캠페인이 만료된 링크는 `campaign: null` 로 반환됩니다.

---

## 비즈니스 규칙

### 링크 발급 검증 순서

```
1. 크리에이터 존재 확인        → CREATOR_NOT_FOUND (404)
2. 상품 존재 확인              → PRODUCT_NOT_FOUND (404)
3. 활성 캠페인 존재 확인       → CAMPAIGN_NOT_ACTIVE (400)
   - isActive = true
   - startsAt ≤ 현재 시각 ≤ endsAt
4. 이미 링크 존재 시 멱등 반환 → 기존 링크 그대로 반환 (201)
5. shortCode 생성 후 저장
```

### 멱등성

크리에이터 × 상품 조합은 DB UNIQUE 제약 (`links_creator_product_uniq`)으로 보장됩니다.
서비스 레이어에서 중복 발급 요청 시 기존 링크를 반환합니다.

### shortCode 생성

- 문자셋: `[A-Za-z0-9]` 62자
- 길이: 8자리 → 약 2,183억 가지 조합
- 생성기: `SecureRandom` 사용
- 충돌 방지: DB 조회 후 중복 시 재생성 루프

---

## 의존성

```
tracking.LinkService
    → identity.CreatorService     (tracking → identity ✅)
    → catalog.ProductService      (tracking → catalog ✅)
    → catalog.CampaignService     (tracking → catalog ✅)
    → tracking.LinkRepository     (자기 도메인 ✅)
```

> 타 도메인 Repository 직접 호출 금지 규칙 준수.
> 모든 타 도메인 접근은 해당 도메인 Service를 통해 이루어집니다.

---

## 서비스 메서드

| 메서드 | 반환 | 용도 |
|--------|------|------|
| `issueLink(creatorId, productId)` | `LinkResponse` (DTO) | Controller 호출용 |
| `getMyLinks(creatorId)` | `List<LinkResponse>` (DTO) | Controller 호출용 |
| `findEntityByShortCode(shortCode)` | `Link` (Entity) | 2단계 ClickService 호출용 |

---

## TODO (다음 단계)

- [ ] **2단계**: `GET /r/{shortCode}` 리다이렉트 + 클릭 추적 (`ClickService`)
- [ ] **Auth 연동**: `@RequestParam creatorId` → `@AuthenticationPrincipal` 교체
