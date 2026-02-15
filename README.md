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

```
src/
├── main/
│   ├── java/com/dooring/
│   │   └── DooringBffApplication.java
│   └── resources/
│       └── application.yml
└── test/
    └── java/com/dooring/
        └── DooringBffApplicationTests.java
```

## 설정

기본 서버 포트: `8080`

Actuator 엔드포인트:
- `/actuator/health` - 헬스 체크
- `/actuator/info` - 애플리케이션 정보
- `/actuator/metrics` - 메트릭 정보