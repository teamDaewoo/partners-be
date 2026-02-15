# domain 패키지

## 역할
**비즈니스 규칙과 모델을 관리하는 핵심 레이어.**
모든 비즈니스 로직은 이곳에 존재하며, 도메인별로 독립적으로 관리됨.

## 도메인 분류

### 1. CRUD 도메인 (단순 엔티티 관리)
**테이블 1:1 매핑, Repository로 CRUD, Service에서 비즈니스 규칙 처리**

- **identity**: 판매자/크리에이터 계정 관리
- **catalog**: 플랫폼/스토어/상품/캠페인 관리
- **tracking**: 링크/클릭/세션/픽셀 이벤트 추적
- **order**: 주문 수신 및 저장

### 2. Aggregate 도메인 (정합성 중요)
**여러 엔티티를 묶어 원자적으로 관리, Port로 DB 접근**

- **attribution**: 구매 귀속 및 커미션 정산 (돈 관련 핵심 로직)

### 3. dataquery 도메인 (읽기 전용)
**여러 도메인 데이터를 횡단 조회 및 집계**

- **dashboard**: 판매자/크리에이터 대시보드

## 도메인 간 의존성 규칙

### 의존 방향 (하위 → 상위만 허용)
```
identity (독립)
   ▲
catalog
   ▲
tracking
   ▲
order
   ▲
attribution (Aggregate)
   ▲
dashboard (dataquery)
```

### 도메인 간 접근 방식
```java
// ✅ 허용: 다른 도메인 Service 호출
@Service
public class AttributionService {
    private final ClickTrackingService clickTrackingService;
    private final CampaignService campaignService;

    public void process() {
        Click click = clickTrackingService.findByToken(token);
        Campaign campaign = campaignService.findActiveByProduct(productId);
    }
}

// ❌ 금지: 다른 도메인 Repository 직접 호출
@Service
public class AttributionService {
    private final ClickRepository clickRepository;  // ❌ 금지!
}

// ❌ 금지: 다른 도메인 Entity를 필드로 참조
@Entity
public class Attribution {
    @ManyToOne
    private Campaign campaign;  // ❌ 금지! Long campaignId로만 참조
}
```

## 내부 구조

### CRUD 도메인 구조
```
domain/{feature}/
├── entity/          # JPA Entity (@Entity)
├── repository/      # Spring Data JPA Repository
├── service/         # 비즈니스 규칙 + @Transactional + DTO 변환
└── dto/             # Request/Response DTO
```

**의존 방향:**
```
entity ← repository ← service → dto
                      service → 다른 도메인 service (허용)
```

### Aggregate 도메인 구조
```
domain/attribution/
├── aggregate/       # Aggregate Root + 소유 엔티티
├── port/            # 저장/조회 인터페이스 (infrastructure에서 구현)
├── service/         # 트랜잭션 주인 + 오케스트레이션
└── dto/             # Response DTO
```

**의존 방향:**
```
aggregate ← service → port (인터페이스)
                      service → 다른 도메인 service (허용)
```

### dataquery 도메인 구조
```
domain/dashboard/
├── querymodel/      # 조회 전용 모델 (비즈니스 집계 로직)
├── port/            # QueryPort 인터페이스
├── service/         # 집계 로직 + DTO 변환
└── dto/             # Response DTO
```

**의존 방향:**
```
querymodel ← service → port (인터페이스)
service → 다른 도메인 service ❌ 금지
```

## 서비스 메서드 네이밍 규칙

```java
@Service
public class CampaignService {

    // Controller용 - DTO 반환
    @Transactional(readOnly = true)
    public CampaignResponse getById(Long id) {
        Campaign campaign = findEntityById(id);
        return CampaignResponse.from(campaign);
    }

    // 다른 도메인 서비스용 - Entity 반환
    @Transactional(readOnly = true)
    public Campaign findActiveByProduct(Long productId) {
        return campaignRepository.findByProductIdAndIsActiveTrue(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_ACTIVE_CAMPAIGN));
    }
}
```

- **`get*()`**: DTO 반환 (외부 노출용)
- **`find*()`**: Entity 반환 (내부용)

## 트랜잭션 규칙

```java
// 쓰기 작업
@Transactional
public CampaignResponse create(...) { ... }

// 읽기 작업
@Transactional(readOnly = true)
public CampaignResponse getById(...) { ... }
```

- **모든 도메인 서비스 메서드에 `@Transactional` 필수**
- 단독 호출 시 → 자기 트랜잭션
- 다른 서비스에서 호출 시 → 상위 트랜잭션에 참여 (REQUIRED 전파)

## 주의 사항

### 즉시 리팩토링 대상
```
❌ 다른 도메인 Repository를 import
❌ 다른 도메인 Entity를 필드로 참조 (@ManyToOne 등)
❌ 서비스 상호참조 (A → B → A)
❌ dashboard에서 도메인 Service 호출
❌ Controller에서 비즈니스 로직 작성
```

### 소유 관계 판단 기준
```
"이 엔티티를 단독으로 CRUD 하는가?"
  YES → 독립 엔티티 (자기 Repository)
  NO  → 상위 엔티티에 포함 (@OneToMany)
```

**예시:**
- `Order` → 독립 (웹훅으로 단독 수신)
- `OrderItem` → Order가 소유 (@OneToMany)
- `Attribution` → 독립 (Aggregate Root)
- `CommissionLedger` → Attribution이 소유

## 5초 판단 가이드
```
1. 단일 테이블 CRUD?              → 해당 CRUD 도메인
2. 여러 도메인 횡단 (쓰기)?       → Aggregate 승격 검토
3. 여러 도메인 횡단 (읽기)?       → dashboard (dataquery)
4. 상호참조 발생?                  → Aggregate로 추출
5. 정합성/멱등성 중요?             → Aggregate 검토
```
