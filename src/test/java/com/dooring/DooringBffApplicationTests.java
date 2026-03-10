package com.dooring;

import com.dooring.domain.catalog.entity.Campaign;
import com.dooring.domain.catalog.entity.Product;
import com.dooring.domain.catalog.entity.Store;
import com.dooring.domain.catalog.repository.CampaignRepository;
import com.dooring.domain.catalog.repository.ProductRepository;
import com.dooring.domain.catalog.repository.StoreRepository;
import com.dooring.domain.identity.entity.Platform;
import com.dooring.domain.identity.repository.PlatformRepository;
import com.dooring.domain.identity.dto.CreatorSignupRequest;
import com.dooring.domain.identity.dto.LoginRequest;
import com.dooring.domain.identity.dto.SellerSignupRequest;
import com.dooring.domain.identity.dto.SignupResponse;
import com.dooring.domain.identity.service.CreatorAuthService;
import com.dooring.domain.identity.service.SellerAuthService;
import com.dooring.domain.tracking.dto.ClickRecordResult;
import com.dooring.domain.tracking.dto.LinkResponse;
import com.dooring.domain.tracking.entity.Click;
import com.dooring.domain.tracking.entity.PixelEvent;
import com.dooring.domain.tracking.repository.AttributionSessionRepository;
import com.dooring.domain.tracking.repository.ClickRepository;
import com.dooring.domain.tracking.repository.LinkRepository;
import com.dooring.domain.tracking.repository.PixelEventRepository;
import com.dooring.domain.tracking.service.ClickTrackingService;
import com.dooring.domain.tracking.service.LinkService;
import com.dooring.domain.tracking.service.PixelTrackingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 전체 어필리에이트 플로우 통합 테스트
 * 전제 조건:
 *   - Docker PostgreSQL (localhost:5432/dooring) 실행 중
 *   - Docker Redis (localhost:6379) 실행 중
 * 테스트 격리:
 *   - @Transactional → 각 테스트 후 DB 자동 롤백
 *   - Redis 키는 @AfterEach에서 수동 정리
 */
@SpringBootTest
@ActiveProfiles("local")
@Transactional
class DooringBffApplicationTests {

    // ── Services ──────────────────────────────────────────────────────────────

    @Autowired private CreatorAuthService creatorAuthService;
    @Autowired private SellerAuthService sellerAuthService;
    @Autowired private LinkService linkService;
    @Autowired private ClickTrackingService clickTrackingService;
    @Autowired private PixelTrackingService pixelTrackingService;

    // ── Repositories (setup & assertion) ──────────────────────────────────────

    @Autowired private PlatformRepository platformRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CampaignRepository campaignRepository;
    @Autowired private LinkRepository linkRepository;
    @Autowired private ClickRepository clickRepository;
    @Autowired private AttributionSessionRepository attributionSessionRepository;
    @Autowired private PixelEventRepository pixelEventRepository;

    // ── Redis ──────────────────────────────────────────────────────────────────

    @Autowired private StringRedisTemplate redisTemplate;

    // ── Test fixture IDs ───────────────────────────────────────────────────────

    private Long sellerId;
    private Long creatorId;
    private Long storeId;
    private Long productId;

    private static final String TEST_SELLER_EMAIL  = "test-seller@dooring-test.io";
    private static final String TEST_CREATOR_EMAIL = "test-creator@dooring-test.io";
    private static final String TEST_PASSWORD      = "Test1234!";

    // ── Setup / Teardown ───────────────────────────────────────────────────────

    /**
     * 각 테스트 전 더미 데이터 세팅
     *   - Seller / Creator : AuthService를 통해 회원가입 (BCrypt 패스워드 적용)
     *   - Platform / Store / Product / Campaign : Repository 직접 저장 (Catalog API 미완성)
     */
    @BeforeEach
    void setUp() {
        // 1. 셀러 회원가입
        SignupResponse sellerResp = sellerAuthService.signup(
                new SellerSignupRequest(TEST_SELLER_EMAIL, "테스트셀러", TEST_PASSWORD));
        sellerId = sellerResp.id();

        // 2. 크리에이터 회원가입
        SignupResponse creatorResp = creatorAuthService.signup(
                new CreatorSignupRequest(TEST_CREATOR_EMAIL, "테스트크리에이터", TEST_PASSWORD));
        creatorId = creatorResp.id();

        // 3. 플랫폼
        Platform platform = platformRepository.save(
                Platform.builder()
                        .code("CUSTOM-TEST")
                        .name("테스트 커스텀몰")
                        .build());

        // 4. 스토어
        Store store = storeRepository.save(
                Store.builder()
                        .sellerId(sellerId)
                        .platformId(platform.getId())
                        .externalStoreId("test-store-001")
                        .name("테스트 스토어")
                        .build());
        storeId = store.getId();

        // 5. 상품 (productUrl 필수 — 클릭 시 리다이렉트 대상)
        Product product = productRepository.save(
                Product.builder()
                        .store(store)
                        .externalProductId("test-prod-001")
                        .name("테스트 상품")
                        .productUrl("https://example.com/product/1")
                        .price(new BigDecimal("29900"))
                        .build());
        productId = product.getId();

        // 6. 캠페인 (최소 90일 이상 필수)
        campaignRepository.save(
                Campaign.builder()
                        .product(product)
                        .sellerId(sellerId)
                        .commissionAmount(new BigDecimal("3000"))
                        .commissionRate(new BigDecimal("0.05"))
                        .startsAt(LocalDateTime.now())
                        .endsAt(LocalDateTime.now().plusDays(91))
                        .build());
    }

    /**
     * Redis는 @Transactional 롤백 대상이 아니므로 수동 정리
     */
    @AfterEach
    void tearDownRedis() {
        redisTemplate.delete("refresh:creator:" + creatorId);
        redisTemplate.delete("refresh:seller:" + sellerId);
    }

    // ── Tests ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Spring Context 로딩 확인")
    void contextLoads() {
        // 컨텍스트가 정상적으로 올라오면 통과
    }

    @Test
    @DisplayName("전체 어필리에이트 플로우: 로그인 → 링크발급 → 클릭 → 픽셀이벤트 → DB 검증")
    void ALL_Flow_Test() {
        // ── 1. 크리에이터 로그인 ─────────────────────────────────────────────
        var loginResult = creatorAuthService.login(
                new LoginRequest(TEST_CREATOR_EMAIL, TEST_PASSWORD));

        assertThat(loginResult.accessToken()).isNotBlank();
        assertThat(loginResult.refreshToken()).isNotBlank();

        // ── 2. 어필리에이트 링크 발급 ────────────────────────────────────────
        LinkResponse linkResp = linkService.issueLink(creatorId, productId);

        assertThat(linkResp.getShortCode()).isNotBlank();
        assertThat(linkResp.getShortUrl()).contains(linkResp.getShortCode());
        assertThat(linkResp.getCampaign()).isNotNull();
        assertThat(linkResp.getCampaign().getCommissionAmount())
                .isEqualByComparingTo("3000");

        // ── 3. 클릭 기록 ─────────────────────────────────────────────────────
        ClickRecordResult clickResult = clickTrackingService.recordClick(
                linkResp.getShortCode(), "127.0.0.1", "TestAgent/1.0");

        assertThat(clickResult.sessionToken()).isNotBlank();
        assertThat(clickResult.redirectUrl())
                .contains("example.com/product/1")
                .contains("dooring_session=");

        // ── 4. DB 검증: clicks 테이블 ────────────────────────────────────────
        List<Click> clicks = clickRepository.findAll();
        assertThat(clicks).hasSize(1);

        Click savedClick = clicks.getFirst();
        assertThat(savedClick.getCampaignId()).isNotNull();
        assertThat(savedClick.getCommissionSnapshotAmount())
                .isEqualByComparingTo("3000");
        assertThat(savedClick.getCommissionSnapshotRate())
                .isEqualByComparingTo("0.05");
        assertThat(savedClick.getClickToken()).isNotBlank();

        // ── 5. DB 검증: attribution_sessions 테이블 ──────────────────────────
        var sessions = attributionSessionRepository.findAll();
        assertThat(sessions).hasSize(1);

        var savedSession = sessions.getFirst();
        assertThat(savedSession.getSessionToken()).isEqualTo(clickResult.sessionToken());
        assertThat(savedSession.getExpiresAt()).isAfter(LocalDateTime.now());

        // ── 6. 픽셀 이벤트 수신 (구매 완료 시뮬레이션) ───────────────────────
        pixelTrackingService.recordPixelEvent(
                storeId, "ORDER-TEST-001", clickResult.sessionToken());

        // ── 7. DB 검증: pixel_events 테이블 ──────────────────────────────────
        List<PixelEvent> pixelEvents = pixelEventRepository.findAll();
        assertThat(pixelEvents).hasSize(1);

        PixelEvent savedPixelEvent = pixelEvents.getFirst();
        assertThat(savedPixelEvent.getExternalOrderId()).isEqualTo("ORDER-TEST-001");
        assertThat(savedPixelEvent.getAttributionSession()).isNotNull();
        assertThat(savedPixelEvent.getAttributionSession().getSessionToken())
                .isEqualTo(clickResult.sessionToken());
    }

    @Test
    @DisplayName("링크 발급 멱등성: 같은 크리에이터+상품으로 두 번 발급하면 동일 shortCode 반환")
    void linkIssuance_idempotency() {
        LinkResponse first  = linkService.issueLink(creatorId, productId);
        LinkResponse second = linkService.issueLink(creatorId, productId);

        assertThat(first.getShortCode()).isEqualTo(second.getShortCode());
        assertThat(first.getLinkId()).isEqualTo(second.getLinkId());
        assertThat(linkRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("픽셀 이벤트 멱등성: 같은 주문 ID로 두 번 전송하면 1건만 저장")
    void pixelEvent_idempotency() {
        pixelTrackingService.recordPixelEvent(storeId, "ORDER-DUPE-001", null);
        pixelTrackingService.recordPixelEvent(storeId, "ORDER-DUPE-001", null);

        assertThat(pixelEventRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("미귀속 픽셀 이벤트: sessionToken 없이도 PixelEvent 저장 (attributionSession = null)")
    void pixelEvent_withoutSession_savedAsOrphan() {
        pixelTrackingService.recordPixelEvent(storeId, "ORDER-NO-SESSION-001", null);

        List<PixelEvent> events = pixelEventRepository.findAll();
        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getAttributionSession()).isNull();
    }

    @Test
    @DisplayName("클릭 추적: 캠페인 스냅샷이 clicks 테이블에 저장된다")
    void click_campaignSnapshot_savedCorrectly() {
        LinkResponse linkResp = linkService.issueLink(creatorId, productId);
        clickTrackingService.recordClick(linkResp.getShortCode(), "10.0.0.1", "Mozilla/5.0");

        Click click = clickRepository.findAll().getFirst();
        assertThat(click.getCampaignId()).isNotNull();
        assertThat(click.getCommissionSnapshotAmount()).isEqualByComparingTo("3000");
        assertThat(click.getIpAddress()).isEqualTo("10.0.0.1");
    }

    @Test
    @DisplayName("토큰 갱신: refresh 후 새 AT/RT 발급, Redis 업데이트 확인")
    void tokenRefresh_issuesNewTokens() {
        var loginResult = creatorAuthService.login(
                new LoginRequest(TEST_CREATOR_EMAIL, TEST_PASSWORD));

        var refreshResult = creatorAuthService.refresh(loginResult.refreshToken());

        assertThat(refreshResult.accessToken()).isNotBlank();
        assertThat(refreshResult.refreshToken()).isNotBlank();
        // RT Rotation: 새 RT가 기존과 달라야 함
        assertThat(refreshResult.refreshToken())
                .isNotEqualTo(loginResult.refreshToken());
    }
}
