# infrastructure 패키지

## 역할
**도메인의 Port 인터페이스를 구현하고, 외부 시스템과의 연동을 담당.**
기술적 세부사항을 캡슐화하여 도메인이 순수하게 유지되도록 함.

## 구조
```
infrastructure/
├── persistence/     # DB 접근 구현체 (Port 구현)
│   ├── attribution/
│   ├── dashboard/
│   └── config/
├── external/        # 외부 API 연동
│   ├── platform/
│   └── payment/
└── security/        # 보안 설정 (JWT, Spring Security)
```

## 1. persistence (DB 접근 계층)

### 역할
**도메인의 Port 인터페이스를 구현하여 실제 DB 접근 로직 제공.**

### 구조
```
persistence/
├── attribution/
│   └── AttributionPersistenceAdapter.java   # Port 구현체
├── dashboard/
│   ├── CreatorReportQueryAdapter.java       # QueryPort 구현체
│   └── SellerReportQueryAdapter.java
└── config/
    └── QueryDslConfig.java                  # QueryDSL 설정
```

### Port 구현 예시
```java
@Repository
@RequiredArgsConstructor
public class AttributionPersistenceAdapter
        implements AttributionWriter, AttributionReader {

    private final EntityManager em;

    @Override
    public void save(Attribution attribution) {
        // Native SQL로 원자적 저장
        em.createNativeQuery("""
            INSERT INTO attributions (order_id, click_id, campaign_id, attributed_at)
            VALUES (:orderId, :clickId, :campaignId, :attributedAt)
        """)
        .setParameter("orderId", attribution.getOrderId())
        .setParameter("clickId", attribution.getClickId())
        .setParameter("campaignId", attribution.getCampaignId())
        .setParameter("attributedAt", attribution.getAttributedAt())
        .executeUpdate();

        // CommissionLedger도 함께 저장
        CommissionLedger ledger = attribution.getCommissionLedger();
        em.createNativeQuery("""
            INSERT INTO commission_ledgers
                (attribution_id, campaign_id, creator_id, seller_id, amount, status)
            VALUES
                (currval('attributions_id_seq'), :campaignId, :creatorId,
                 :sellerId, :amount, :status)
        """)
        .setParameter("campaignId", ledger.getCampaignId())
        .setParameter("creatorId", ledger.getCreatorId())
        .setParameter("sellerId", ledger.getSellerId())
        .setParameter("amount", ledger.getAmount())
        .setParameter("status", ledger.getStatus().name())
        .executeUpdate();
    }

    @Override
    public boolean existsByOrderId(Long orderId) {
        Long count = em.createQuery(
                "SELECT COUNT(a) FROM AttributionJpaEntity a WHERE a.orderId = :orderId",
                Long.class)
                .setParameter("orderId", orderId)
                .getSingleResult();
        return count > 0;
    }
}
```

### QueryPort 구현 예시 (QueryDSL)
```java
@Repository
@RequiredArgsConstructor
public class CreatorReportQueryAdapter implements CreatorReportQueryPort {

    private final JPAQueryFactory queryFactory;

    @Override
    public CreatorReport findByCreatorId(Long creatorId,
                                         LocalDate startDate,
                                         LocalDate endDate) {
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
                        click.clickedAt.between(
                                startDate.atStartOfDay(),
                                endDate.atTime(23, 59, 59))
                )
                .groupBy(creator.id, creator.name)
                .fetchOne();
    }
}
```

### config (기술 설정)
```java
@Configuration
public class QueryDslConfig {

    @PersistenceContext
    private EntityManager entityManager;

    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(entityManager);
    }
}
```

## 2. external (외부 시스템 연동)

### 역할
**외부 API 및 서비스와의 통신을 캡슐화.**

### 구조
```
external/
├── platform/
│   ├── cafe24/
│   │   └── Cafe24ApiClient.java    # Cafe24 API 클라이언트
│   └── imweb/
│       └── ImwebApiClient.java     # 아임웹 API 클라이언트
└── payment/
    └── PaymentGateway.java         # 결제 시스템 연동
```

### 외부 API 클라이언트 예시
```java
@Component
@RequiredArgsConstructor
public class Cafe24ApiClient {

    private final RestTemplate restTemplate;

    @Value("${cafe24.api.url}")
    private String apiUrl;

    @Value("${cafe24.api.key}")
    private String apiKey;

    public List<Product> fetchProducts(String mallId) {
        String url = apiUrl + "/products?mall_id=" + mallId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Cafe24ProductResponse> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity,
                            Cafe24ProductResponse.class);

            return response.getBody().toProducts();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.CAFE24_API_ERROR, e);
        }
    }

    public void registerWebhook(String mallId, String callbackUrl) {
        // 웹훅 등록 로직
    }
}
```

### Port 패턴 적용 (선택적)
외부 시스템도 Port로 추상화할 수 있음:

```java
// domain/catalog/port/
public interface ProductSyncPort {
    List<Product> fetchProducts(String platformId, String storeId);
}

// infrastructure/external/platform/cafe24/
@Component
public class Cafe24ProductSyncAdapter implements ProductSyncPort {
    @Override
    public List<Product> fetchProducts(String platformId, String storeId) {
        // Cafe24 API 호출
    }
}
```

## 3. security (보안 설정)

### 역할
**인증/인가 및 보안 관련 기술 구성.**

### 구조
```
security/
├── JwtTokenProvider.java    # JWT 토큰 생성/검증
└── SecurityConfig.java       # Spring Security 설정
```

### JWT Provider 예시
```java
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expiration;

    public String generateToken(Long userId, String role) {
        Claims claims = Jwts.claims().setSubject(String.valueOf(userId));
        claims.put("role", role);

        Date now = new Date();
        Date validity = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long getUserId(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(token)
                .getBody();
        return Long.parseLong(claims.getSubject());
    }
}
```

### Security Config 예시
```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/seller/**").hasRole("SELLER")
                        .requestMatchers("/api/creator/**").hasRole("CREATOR")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
```

## 의존성 방향

```
domain (Port 인터페이스 정의)
   ▲
   │ (구현)
   │
infrastructure (Port 구현체)
```

**핵심**: 도메인이 infrastructure를 몰라야 함 (의존성 역전)

## 주요 기술 스택

### persistence
- **JPA/Hibernate**: ORM
- **QueryDSL**: 타입 안전 쿼리
- **Native SQL**: 복잡한 쿼리/성능 최적화

### external
- **RestTemplate** / **WebClient**: HTTP 클라이언트
- **Feign Client**: 선언적 HTTP 클라이언트 (선택적)

### security
- **Spring Security**: 인증/인가 프레임워크
- **JWT**: 토큰 기반 인증

## 주의 사항

### ✅ 허용
- Port 인터페이스 구현
- 외부 API 호출 캡슐화
- 기술적 세부사항 처리
- Native SQL/QueryDSL 자유롭게 사용

### ❌ 금지
- 비즈니스 로직 포함 (도메인 레이어 책임)
- 다른 infrastructure 모듈 직접 참조
- Controller/Service 직접 의존

## 테스트 전략

### Port 인터페이스 테스트
```java
@DataJpaTest
class AttributionPersistenceAdapterTest {

    @Autowired
    private AttributionPersistenceAdapter adapter;

    @Test
    void save_성공() {
        // Given
        Attribution attribution = new Attribution(...);

        // When
        adapter.save(attribution);

        // Then
        assertTrue(adapter.existsByOrderId(attribution.getOrderId()));
    }
}
```

### 외부 API 테스트 (MockServer)
```java
@SpringBootTest
class Cafe24ApiClientTest {

    @Autowired
    private Cafe24ApiClient client;

    @MockBean
    private RestTemplate restTemplate;

    @Test
    void fetchProducts_성공() {
        // Given
        when(restTemplate.exchange(anyString(), any(), any(), any()))
                .thenReturn(ResponseEntity.ok(mockResponse));

        // When
        List<Product> products = client.fetchProducts("mall123");

        // Then
        assertThat(products).hasSize(10);
    }
}
```

## 확장 시나리오

### 새로운 외부 플랫폼 추가
1. `external/platform/{platform}/` 디렉토리 생성
2. API 클라이언트 구현
3. Port 인터페이스 정의 (필요 시)
4. 도메인 서비스에서 사용

### 새로운 Port 구현 추가
1. `domain/{feature}/port/` 에 인터페이스 정의
2. `infrastructure/persistence/{feature}/` 에 Adapter 구현
3. 도메인 서비스에서 Port 주입받아 사용
