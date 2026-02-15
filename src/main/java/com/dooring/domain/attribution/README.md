# attribution 패키지 (Aggregate 도메인)

## 역할
**구매 귀속(Attribution)과 커미션 정산을 관리하는 핵심 도메인.**
정합성과 멱등성이 최우선이며, DDD Aggregate 패턴을 적용.

## 왜 Aggregate인가?
- **정합성**: Attribution과 CommissionLedger는 항상 원자적으로 함께 생성되어야 함
- **멱등성**: 동일 주문에 대해 중복 귀속 방지 필요
- **복잡한 비즈니스 규칙**: 여러 도메인(click, campaign, order)을 횡단하여 검증
- **돈 관련**: 커미션 금액 계산 및 상태 관리

## 구조
```
attribution/
├── aggregate/       # Aggregate Root + 소유 엔티티
│   ├── Attribution.java           # Aggregate Root
│   ├── CommissionLedger.java      # Attribution이 소유
│   └── CommissionStatus.java      # Enum
├── port/            # DB 접근 인터페이스 (의존성 역전)
│   ├── AttributionWriter.java     # 저장 인터페이스
│   └── AttributionReader.java     # 조회 인터페이스
├── service/         # 트랜잭션 주인 + 오케스트레이션
│   └── AttributionService.java
└── dto/             # Response DTO
    └── AttributionResult.java
```

## 의존성 방향
```
aggregate ← service → port (인터페이스)
                      ↓
            다른 도메인 service 호출 (허용)
                      ↓
          ClickTrackingService
          CampaignService
          OrderService
```

**Port 구현체는 `infrastructure/persistence/attribution/`에 위치**

## Aggregate Root (Attribution)

### 책임
- 귀속 데이터의 불변식(invariant) 유지
- CommissionLedger 생성 및 관리
- 비즈니스 규칙 강제

### 특징
- **상태 전이 로직 포함**: 커미션 확정/취소 등
- **소유 엔티티 직접 생성**: CommissionLedger는 외부에서 생성 불가
- **불변성**: 생성 후 핵심 필드 변경 불가

## Port (의존성 역전)

### 왜 Port를 사용하는가?
- **도메인 순수성 유지**: Aggregate가 JPA/DB에 의존하지 않음
- **테스트 용이성**: 인메모리 구현체로 쉽게 테스트 가능
- **유연성**: DB 구현을 쉽게 교체 가능

### Port 인터페이스
```java
// 저장 인터페이스
public interface AttributionWriter {
    void save(Attribution attribution);
}

// 조회 인터페이스
public interface AttributionReader {
    boolean existsByOrderId(Long orderId);
    Optional<Attribution> findByOrderId(Long orderId);
}
```

**구현체 위치**: `infrastructure/persistence/attribution/AttributionPersistenceAdapter.java`

## Service (트랜잭션 주인)

### 책임
- **트랜잭션 경계 설정** (`@Transactional`)
- **여러 도메인 서비스 오케스트레이션**
- **비즈니스 규칙 검증**
- **Aggregate 생성 및 저장**

### 코드 예시
```java
@Service
@RequiredArgsConstructor
public class AttributionService {

    private final AttributionWriter attributionWriter;
    private final AttributionReader attributionReader;

    // 다른 도메인 서비스
    private final ClickTrackingService clickTrackingService;
    private final CampaignService campaignService;
    private final LinkService linkService;

    @Transactional
    public AttributionResult process(String clickToken, Long orderId) {
        // 1. 멱등성 검증
        if (attributionReader.existsByOrderId(orderId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_ATTRIBUTION);
        }

        // 2. 다른 도메인 데이터 조회 (Service 경유)
        Click click = clickTrackingService.findByToken(clickToken);
        Campaign campaign = campaignService.findActiveByProduct(click.getProductId());
        Link link = linkService.findById(click.getLinkId());

        // 3. 비즈니스 규칙 검증
        validateAttributionWindow(click);
        validateCampaignPeriod(campaign, click.getClickedAt());

        // 4. Aggregate 생성 (CommissionLedger도 내부에서 생성)
        Attribution attribution = new Attribution(
                orderId,
                click.getId(),
                campaign.getId(),
                link.getCreatorId(),
                campaign.getSellerId(),
                campaign.getCommissionAmount()
        );

        // 5. 저장 (Port 사용)
        attributionWriter.save(attribution);

        return AttributionResult.from(attribution);
    }

    private void validateAttributionWindow(Click click) {
        if (click.getClickedAt().plusHours(24).isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.ATTRIBUTION_WINDOW_EXPIRED);
        }
    }

    private void validateCampaignPeriod(Campaign campaign, LocalDateTime clickedAt) {
        if (clickedAt.isBefore(campaign.getStartsAt()) ||
            clickedAt.isAfter(campaign.getEndsAt())) {
            throw new BusinessException(ErrorCode.CAMPAIGN_NOT_IN_PERIOD);
        }
    }
}
```

## 멱등성 보장 전략

### 1차 방어: 서비스 레벨
```java
if (attributionReader.existsByOrderId(orderId)) {
    throw new BusinessException(ErrorCode.DUPLICATE_ATTRIBUTION);
}
```

### 2차 방어: DB 레벨
```sql
CREATE UNIQUE INDEX idx_attributions_order_id ON attributions(order_id);
```

### 충돌 처리
- UNIQUE 제약 위반 시 → 이미 처리됨으로 간주 (멱등 성공)
- 동시 요청 → 하나만 성공, 나머지는 멱등 응답

## Aggregate vs CRUD 도메인 비교

| 항목 | CRUD 도메인 | Aggregate 도메인 |
|------|------------|------------------|
| 복잡도 | 단순 (테이블 1:1) | 복잡 (여러 엔티티 묶음) |
| DB 접근 | Repository 직접 | Port 인터페이스 |
| 트랜잭션 | 단일 테이블 | 여러 테이블 원자 저장 |
| 비즈니스 규칙 | 간단한 검증 | 복잡한 상태전이/불변식 |
| 정합성 요구 | 보통 | 높음 (돈/정산) |
| 예시 | Campaign, Product | Attribution |

## 주의 사항

### ✅ 허용
- 다른 도메인 Service 호출
- Port를 통한 DB 접근
- Aggregate 내부에서 소유 엔티티 생성

### ❌ 금지
- Repository 직접 사용 (Port 사용 필수)
- 다른 도메인 Entity를 필드로 참조
- Aggregate 외부에서 CommissionLedger 생성
- 트랜잭션 없이 저장

## 확장 시나리오

### 새로운 Aggregate 후보
다음 조건을 만족하면 Aggregate 승격 검토:
- 여러 도메인을 횡단하는 쓰기 작업
- 원자적으로 함께 관리되어야 하는 엔티티 그룹
- 높은 정합성/멱등성 요구
- 복잡한 비즈니스 규칙 포함
- 돈/정산 관련

**예시**: 정산(Settlement), 환불(Refund) 등
