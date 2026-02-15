# dashboard 패키지 (dataquery 도메인)

## 역할
**여러 도메인의 데이터를 횡단 조회하여 집계/분석 결과를 제공.**
읽기 전용 도메인으로, 쓰기 작업 없음.

## 왜 별도 도메인인가?
- **횡단 조회**: 여러 CRUD 도메인의 데이터를 JOIN하여 조회
- **복잡한 집계**: 단순 조회가 아닌 비즈니스 로직을 포함한 집계
- **성능 최적화**: QueryDSL/Native SQL로 읽기 최적화
- **변경 이유 분리**: 대시보드 요구사항은 CRUD 도메인과 다른 속도로 변경

## 구조
```
dashboard/
├── querymodel/      # 조회 전용 모델 (비즈니스 집계 로직 포함)
│   ├── CreatorReport.java         # 크리에이터 리포트
│   ├── LinkPerformance.java       # 링크별 성과
│   ├── SellerReport.java          # 판매자 리포트
│   └── CampaignPerformance.java   # 캠페인별 성과
├── port/            # QueryPort 인터페이스 (infrastructure에서 구현)
│   ├── CreatorReportQueryPort.java
│   └── SellerReportQueryPort.java
├── service/         # 집계 로직 + DTO 변환
│   ├── CreatorDashboardService.java
│   └── SellerDashboardService.java
└── dto/             # Response DTO
    ├── CreatorDashboardResponse.java
    └── SellerDashboardResponse.java
```

## 의존성 방향
```
querymodel ← service → port (인터페이스)

service → 다른 도메인 service ❌ 금지
```

**Port 구현체는 `infrastructure/persistence/dashboard/`에 위치**

## QueryModel (조회 전용 모델)

### 특징
- **`@Entity` 아님**: JPA Entity가 아닌 순수 Java 객체
- **불변 객체**: 생성 후 변경 불가
- **비즈니스 집계 로직 포함**: 단순 데이터 컨테이너가 아님

### 코드 예시
```java
@Getter
public class CreatorReport {

    private final Long creatorId;
    private final String creatorName;
    private final int totalClicks;
    private final int totalAttributions;
    private final BigDecimal totalCommission;
    private final BigDecimal conversionRate;  // 비즈니스 로직

    public CreatorReport(Long creatorId, String creatorName,
                         int totalClicks, int totalAttributions,
                         BigDecimal totalCommission) {
        this.creatorId = creatorId;
        this.creatorName = creatorName;
        this.totalClicks = totalClicks;
        this.totalAttributions = totalAttributions;
        this.totalCommission = totalCommission;
        // 비즈니스 집계 로직
        this.conversionRate = calculateConversionRate(totalClicks, totalAttributions);
    }

    private BigDecimal calculateConversionRate(int clicks, int attributions) {
        if (clicks == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(attributions)
                .divide(BigDecimal.valueOf(clicks), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    // Value Object (성과 데이터)
    @Getter
    public static class LinkPerformance {
        private final Long linkId;
        private final String shortCode;
        private final int clicks;
        private final int attributions;
        private final BigDecimal commission;

        public LinkPerformance(Long linkId, String shortCode,
                               int clicks, int attributions, BigDecimal commission) {
            this.linkId = linkId;
            this.shortCode = shortCode;
            this.clicks = clicks;
            this.attributions = attributions;
            this.commission = commission;
        }
    }
}
```

## Port (의존성 역전)

### 왜 Port를 사용하는가?
- **도메인 순수성**: QueryModel이 DB 기술에 의존하지 않음
- **성능 최적화**: 구현체에서 Native SQL/QueryDSL 자유롭게 사용
- **테스트 용이성**: 인메모리 구현체로 쉽게 테스트

### Port 인터페이스
```java
public interface CreatorReportQueryPort {
    CreatorReport findByCreatorId(Long creatorId, LocalDate startDate, LocalDate endDate);
    List<LinkPerformance> findLinkPerformances(Long creatorId, LocalDate startDate, LocalDate endDate);
}
```

**구현체 위치**: `infrastructure/persistence/dashboard/CreatorReportQueryAdapter.java`

## Service

### 책임
- **QueryPort 호출하여 데이터 조회**
- **비즈니스 집계 로직 실행** (QueryModel에 위임)
- **DTO 변환**

### ❌ 금지 사항
- **다른 도메인 Service 호출 금지**
- **쓰기 작업 금지**
- **트랜잭션 불필요** (읽기만)

### 코드 예시
```java
@Service
@RequiredArgsConstructor
public class CreatorDashboardService {

    private final CreatorReportQueryPort creatorReportQueryPort;

    @Transactional(readOnly = true)
    public CreatorDashboardResponse getDashboard(Long creatorId,
                                                  LocalDate startDate,
                                                  LocalDate endDate) {
        // QueryPort를 통해 데이터 조회
        CreatorReport report = creatorReportQueryPort.findByCreatorId(
                creatorId, startDate, endDate);

        List<CreatorReport.LinkPerformance> linkPerformances =
                creatorReportQueryPort.findLinkPerformances(creatorId, startDate, endDate);

        // DTO 변환
        return CreatorDashboardResponse.from(report, linkPerformances);
    }
}
```

## 횡단 조회 vs 도메인 Service 호출

### ❌ 잘못된 방식 (다른 도메인 Service 호출)
```java
@Service
public class CreatorDashboardService {
    private final LinkService linkService;           // ❌ 금지
    private final ClickTrackingService clickService; // ❌ 금지

    public CreatorDashboardResponse getDashboard(Long creatorId) {
        // 여러 도메인 서비스 호출 → N+1 문제, 성능 저하
        List<Link> links = linkService.findByCreator(creatorId);
        for (Link link : links) {
            int clicks = clickService.countByLink(link.getId()); // N+1!
        }
    }
}
```

### ✅ 올바른 방식 (QueryPort로 횡단 조회)
```java
@Service
public class CreatorDashboardService {
    private final CreatorReportQueryPort queryPort;  // ✅ Port 사용

    public CreatorDashboardResponse getDashboard(Long creatorId) {
        // 한 번의 JOIN 쿼리로 모든 데이터 조회
        CreatorReport report = queryPort.findByCreatorId(creatorId, start, end);
    }
}
```

## QueryPort 구현 예시 (infrastructure)

```java
@Repository
@RequiredArgsConstructor
public class CreatorReportQueryAdapter implements CreatorReportQueryPort {

    private final JPAQueryFactory queryFactory;

    @Override
    public CreatorReport findByCreatorId(Long creatorId,
                                         LocalDate startDate,
                                         LocalDate endDate) {
        // QueryDSL로 복잡한 JOIN 쿼리
        return queryFactory
                .select(Projections.constructor(CreatorReport.class,
                        creator.id,
                        creator.name,
                        click.count().intValue(),
                        attribution.count().intValue(),
                        commissionLedger.amount.sum()))
                .from(creator)
                .leftJoin(link).on(link.creatorId.eq(creator.id))
                .leftJoin(click).on(click.linkId.eq(link.id))
                .leftJoin(attribution).on(attribution.clickId.eq(click.id))
                .leftJoin(commissionLedger).on(commissionLedger.creatorId.eq(creator.id))
                .where(
                        creator.id.eq(creatorId),
                        click.clickedAt.between(startDate.atStartOfDay(), endDate.atTime(23, 59, 59))
                )
                .groupBy(creator.id, creator.name)
                .fetchOne();
    }
}
```

## dataquery vs CRUD 도메인 비교

| 항목 | CRUD 도메인 | dataquery 도메인 |
|------|------------|------------------|
| 목적 | 쓰기 + 읽기 | 읽기만 |
| 데이터 범위 | 단일 테이블/도메인 | 여러 도메인 횡단 |
| 모델 | JPA Entity | QueryModel (순수 객체) |
| DB 접근 | Repository | QueryPort |
| 성능 최적화 | 제한적 | Native SQL/QueryDSL |
| 비즈니스 로직 | 상태 변경 | 집계/계산 |

## 주의 사항

### ✅ 허용
- QueryPort를 통한 복잡한 JOIN 조회
- QueryModel에 집계 로직 포함
- Native SQL/QueryDSL 사용

### ❌ 금지
- 다른 도메인 Service 호출
- 쓰기 작업 (`@Transactional` 없이)
- JPA Entity 사용
- Repository 직접 사용

## 성능 최적화 전략

### 1. 읽기 전용 최적화
```java
@Transactional(readOnly = true)  // 읽기 최적화 힌트
public CreatorDashboardResponse getDashboard(...) { ... }
```

### 2. Projection 사용
```java
// 필요한 컬럼만 조회
Projections.constructor(CreatorReport.class, creator.id, creator.name, ...)
```

### 3. Batch 조회
```java
// N+1 방지: 한 번의 쿼리로 모든 데이터 조회
queryFactory.selectFrom(...)
    .leftJoin(...).fetchJoin()
    .where(...)
```

### 4. 캐싱 고려
```java
@Cacheable("creator-dashboard")
public CreatorDashboardResponse getDashboard(...) { ... }
```

## 확장 시나리오

### 새로운 대시보드 추가
1. `querymodel/` 에 새로운 QueryModel 추가
2. `port/` 에 새로운 QueryPort 인터페이스 추가
3. `infrastructure/persistence/dashboard/` 에 Adapter 구현
4. `service/` 에 서비스 추가
5. `api/dashboard/` 에 Controller 추가
