# common 패키지

## 역할
**전 레이어에서 공통으로 사용되는 유틸리티, 예외, DTO, 설정 관리.**

## 구조
```
common/
├── dto/             # 공통 응답 DTO
│   ├── ApiResponse.java
│   └── PageResponse.java
├── exception/       # 예외 처리
│   ├── ErrorCode.java
│   ├── BusinessException.java
│   └── GlobalExceptionHandler.java
├── util/            # 유틸리티 클래스
│   ├── ShortCodeGenerator.java
│   └── TokenGenerator.java
└── config/          # 기술 설정
    ├── JpaConfig.java
    └── SwaggerConfig.java
```

## 1. dto (공통 응답 DTO)

### ApiResponse (통일된 API 응답 래퍼)

```java
@Getter
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ErrorInfo error;

    private ApiResponse(boolean success, T data, ErrorInfo error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    // 성공 응답
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null);
    }

    // 실패 응답
    public static <T> ApiResponse<T> fail(ErrorCode errorCode, String message) {
        return new ApiResponse<>(false, null,
                new ErrorInfo(errorCode.name(), message));
    }

    @Getter
    @AllArgsConstructor
    public static class ErrorInfo {
        private final String code;
        private final String message;
    }
}
```

**사용 예시:**
```java
@GetMapping("/{id}")
public ApiResponse<CampaignResponse> getById(@PathVariable Long id) {
    return ApiResponse.ok(campaignService.getById(id));
}
```

### PageResponse (페이징 응답)

```java
@Getter
public class PageResponse<T> {

    private final List<T> content;
    private final int pageNumber;
    private final int pageSize;
    private final long totalElements;
    private final int totalPages;
    private final boolean hasNext;

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }

    private PageResponse(List<T> content, int pageNumber, int pageSize,
                         long totalElements, int totalPages, boolean hasNext) {
        this.content = content;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.hasNext = hasNext;
    }
}
```

## 2. exception (예외 처리)

### ErrorCode (에러 코드 정의)

```java
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(400, "잘못된 입력값입니다."),
    UNAUTHORIZED(401, "인증이 필요합니다."),
    FORBIDDEN(403, "권한이 없습니다."),
    NOT_FOUND(404, "리소스를 찾을 수 없습니다."),

    // Identity
    SELLER_NOT_FOUND(404, "판매자를 찾을 수 없습니다."),
    CREATOR_NOT_FOUND(404, "크리에이터를 찾을 수 없습니다."),
    DUPLICATE_EMAIL(409, "이미 사용 중인 이메일입니다."),

    // Catalog
    CAMPAIGN_NOT_FOUND(404, "캠페인을 찾을 수 없습니다."),
    NO_ACTIVE_CAMPAIGN(404, "활성화된 캠페인이 없습니다."),
    DUPLICATE_ACTIVE_CAMPAIGN(409, "이미 활성화된 캠페인이 존재합니다."),
    CAMPAIGN_MIN_DURATION(400, "캠페인 기간은 최소 90일이어야 합니다."),

    // Attribution
    DUPLICATE_ATTRIBUTION(409, "이미 귀속 처리된 주문입니다."),
    ATTRIBUTION_WINDOW_EXPIRED(400, "귀속 윈도우가 만료되었습니다."),
    CAMPAIGN_NOT_IN_PERIOD(400, "캠페인 기간이 아닙니다."),
    INVALID_COMMISSION_STATUS(400, "잘못된 커미션 상태입니다."),

    // External
    CAFE24_API_ERROR(500, "Cafe24 API 호출 중 오류가 발생했습니다."),
    IMWEB_API_ERROR(500, "아임웹 API 호출 중 오류가 발생했습니다.");

    private final int status;
    private final String message;
}
```

### BusinessException (비즈니스 예외)

```java
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}
```

**사용 예시:**
```java
public Campaign findById(Long id) {
    return campaignRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.CAMPAIGN_NOT_FOUND));
}
```

### GlobalExceptionHandler (전역 예외 처리)

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 비즈니스 예외
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("Business exception: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode, errorCode.getMessage()));
    }

    // Validation 예외
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        log.warn("Validation exception: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(ErrorCode.INVALID_INPUT_VALUE, message));
    }

    // 기타 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unexpected exception", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(
                        ErrorCode.INTERNAL_SERVER_ERROR,
                        "서버 오류가 발생했습니다."
                ));
    }
}
```

## 3. util (유틸리티)

### ShortCodeGenerator (짧은 코드 생성)

```java
@Component
public class ShortCodeGenerator {

    private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final int CODE_LENGTH = 8;

    public String generate() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(CODE_LENGTH);

        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = random.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }

        return sb.toString();
    }

    public String generateUnique(Function<String, Boolean> existsChecker) {
        String code;
        int attempts = 0;
        do {
            code = generate();
            if (++attempts > 10) {
                throw new BusinessException(ErrorCode.CODE_GENERATION_FAILED);
            }
        } while (existsChecker.apply(code));

        return code;
    }
}
```

**사용 예시:**
```java
@Service
public class LinkService {
    private final ShortCodeGenerator shortCodeGenerator;

    public Link create(...) {
        String shortCode = shortCodeGenerator.generateUnique(
                code -> linkRepository.existsByShortCode(code)
        );
        // ...
    }
}
```

### TokenGenerator (토큰 생성)

```java
@Component
public class TokenGenerator {

    public String generateClickToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public String generateApiKey(String prefix) {
        String random = UUID.randomUUID().toString().replace("-", "");
        return prefix + "_" + random;
    }
}
```

## 4. config (기술 설정)

### JpaConfig (JPA 설정)

```java
@Configuration
@EnableJpaAuditing
public class JpaConfig {

    @Bean
    public AuditorAware<Long> auditorProvider() {
        return () -> {
            // Spring Security에서 현재 사용자 ID 가져오기
            Authentication authentication = SecurityContextHolder
                    .getContext()
                    .getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.empty();
            }

            return Optional.of(Long.parseLong(authentication.getName()));
        };
    }
}
```

### SwaggerConfig (API 문서 설정)

```java
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Dooring BFF API")
                        .description("Dooring Partners Backend API 문서")
                        .version("v1.0"))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
```

## 패키지 간 의존성

```
common (독립적, 모든 레이어에서 참조 가능)
   ▲
   │
api, domain, infrastructure (common 참조)
```

## 주의 사항

### ✅ 허용
- 모든 레이어에서 common 참조
- 순수 유틸리티 함수
- 공통 DTO/예외 정의
- 기술 설정

### ❌ 금지
- 비즈니스 로직 포함
- 다른 레이어(api, domain, infrastructure) 참조
- 상태를 가진 클래스 (모두 stateless)

## 원칙

### 1. Stateless
모든 클래스는 상태를 가지지 않음 (유틸리티, 설정만)

### 2. 독립성
다른 레이어를 참조하지 않음

### 3. 재사용성
어떤 프로젝트에도 가져다 쓸 수 있을 정도로 범용적

## 확장 시나리오

### 새로운 ErrorCode 추가
```java
PAYMENT_FAILED(400, "결제에 실패했습니다."),
REFUND_NOT_ALLOWED(400, "환불이 불가능한 상태입니다."),
```

### 새로운 유틸리티 추가
```java
@Component
public class DateTimeUtil {
    public static LocalDateTime toKst(LocalDateTime utc) {
        return utc.atZone(ZoneId.of("UTC"))
                .withZoneSameInstant(ZoneId.of("Asia/Seoul"))
                .toLocalDateTime();
    }
}
```

### 새로운 공통 DTO 추가
```java
@Getter
public class CursorResponse<T> {
    private final List<T> content;
    private final String nextCursor;
    private final boolean hasNext;
}
```
