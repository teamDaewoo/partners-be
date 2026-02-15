# Dooring BFF (Backend For Frontend)

Dooring 서비스를 위한 BFF 서버입니다.

## 기술 스택

### Core
- **Java**: 21 (LTS)
- **Spring Boot**: 3.5.0
- **Build Tool**: Gradle 8.11.1 (Groovy DSL)
- **Packaging**: JAR

### Dependencies
- **Spring Web**: RESTful API 개발
- **Spring Validation**: 요청 데이터 검증
- **Spring Data JPA**: ORM 및 데이터베이스 접근
- **Spring Actuator**: 애플리케이션 모니터링 및 헬스 체크
- **Lombok**: 보일러플레이트 코드 감소

### Test
- **JUnit 5**: 단위 테스트
- **Spring Boot Test**: 통합 테스트

## 시작하기

### 요구사항
- Java 21 이상
- Gradle 8.11.1 이상 (Gradle Wrapper 포함)

### 빌드 및 실행

```bash
# 빌드
./gradlew clean build

# 실행
./gradlew bootRun
```

### 테스트

```bash
./gradlew test
```

## 프로젝트 구조

### 패키지 아키텍처

레이어드 + 도메인 중심 설계를 따르며, DDD-lite 접근 방식을 적용합니다.

```
com.dooring/
│
├── DooringBffApplication.java
│
├── api/                    # HTTP 컨트롤러 계층
│   ├── seller/            # 판매자 API
│   ├── creator/           # 크리에이터 API
│   ├── tracking/          # 트래킹 API
│   ├── webhook/           # 웹훅 수신
│   └── dashboard/         # 대시보드 API
│
├── domain/                # 비즈니스 도메인 계층
│   ├── identity/          # 계정 관리 (CRUD)
│   ├── catalog/           # 상품/캠페인 관리 (CRUD)
│   ├── tracking/          # 트래킹 데이터 (CRUD)
│   ├── order/             # 주문 관리 (CRUD)
│   ├── attribution/       # 구매 귀속/정산 (Aggregate)
│   └── dashboard/         # 대시보드 집계 (dataquery)
│
├── infrastructure/        # 기술 구현 계층
│   ├── persistence/       # DB 접근 (Port 구현)
│   ├── external/          # 외부 API 연동
│   └── security/          # 인증/인가
│
└── common/                # 공통 유틸리티
    ├── dto/               # 공통 응답 DTO
    ├── exception/         # 예외 처리
    ├── util/              # 유틸리티
    └── config/            # 공통 설정
```

### 도메인 유형

프로젝트는 3가지 유형의 도메인으로 구성됩니다:

1. **CRUD 도메인** (identity, catalog, tracking, order)
   - 단순 엔티티 관리
   - Entity - Repository - Service 구조

2. **Aggregate 도메인** (attribution)
   - 복잡한 비즈니스 규칙 및 정합성 보장
   - Aggregate Root - Port - Service 구조

3. **dataquery 도메인** (dashboard)
   - 읽기 전용 횡단 조회
   - QueryModel - QueryPort - Service 구조

### 상세 가이드

각 패키지별 상세한 역할과 규칙은 해당 패키지의 README를 참고하세요:
- `api/README.md` - API 계층 가이드
- `domain/README.md` - 도메인 계층 전체 가이드
- `domain/attribution/README.md` - Aggregate 패턴 가이드
- `domain/dashboard/README.md` - dataquery 패턴 가이드
- `infrastructure/README.md` - 인프라 계층 가이드
- `common/README.md` - 공통 유틸리티 가이드

## 설정

기본 서버 포트: `8080`

Actuator 엔드포인트:
- `/actuator/health` - 헬스 체크
- `/actuator/info` - 애플리케이션 정보
- `/actuator/metrics` - 메트릭 정보