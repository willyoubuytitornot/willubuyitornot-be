# 프로젝트 컨벤션 — willubuyitornot-be

백엔드-QA 하네스의 정식(canonical) 레퍼런스. 모든 에이전트는 코드를 생성하기 전에 이 문서를 읽어,
설계·구현·테스트가 기존 코드베이스와 일관되도록 유지한다.

## 목차
1. 기술 스택
2. 패키지 & 레이어 구조
3. 응답 래퍼 (`@WrapResponse` / `ApiResponse`)
4. 예외 처리
5. 영속성 (MongoDB + ObjectId)
6. 도메인 모델
7. 네이밍 & 스타일 컨벤션
8. 빌드 & 실행 명령어
9. 테스트 컨벤션

---

## 1. 기술 스택

| 항목 | 선택 |
|------|------|
| 언어 | Java 21 (toolchain) |
| 프레임워크 | Spring Boot 3.3.5 (spring-boot-starter-web) |
| 영속성 | Spring Data MongoDB (`spring-boot-starter-data-mongodb`) |
| JSON | Jackson + `jackson-datatype-jsr310` (Java time) |
| 보일러플레이트 | Lombok (`@Getter/@Setter/@NoArgsConstructor/@RequiredArgsConstructor/@FieldDefaults`) |
| 외부 AI | Gemini via `RestTemplate` (`GeminiService`) |
| 빌드 | Gradle (`./gradlew`) |
| 테스트 | JUnit 5 via `spring-boot-starter-test` (`useJUnitPlatform()`) |

베이스 패키지: `com.willu.buyitornot`. MongoDB DB명: `buyit` (host localhost:27017, dev 프로파일).

## 2. 패키지 & 레이어 구조

```
com.willu.buyitornot
├── web.ui            ← @RestController 엔드포인트 (얇게 유지; 서비스에 위임)
│   ├── common        ← ApiResponse<T>, @WrapResponse
│   ├── advice        ← ResponseWrapperAdvice (래퍼), config/WebConfig
├── service           ← 비즈니스 로직 (@Service, @RequiredArgsConstructor)
├── infra.repository  ← Spring Data Mongo 리포지토리 (MongoRepository 상속 인터페이스)
├── infra.collection  ← @Document POJO (영속성 모델)
└── exception         ← 커스텀 예외 + GlobalExceptionHandler
```

**레이어 규칙:** Controller → Service → Repository → Document. 컨트롤러는 리포지토리를 직접
호출하지 않는다. 로직은 서비스에 두고, 서비스는 도메인 예외를 던진다. 컨트롤러는 얇게 유지한다.

## 3. 응답 래퍼

모든 성공 응답은 `ApiResponse<T>`로 감싸진다:
```json
{ "success": true, "data": { ... }, "message": null, "error": null }
```
`@JsonInclude(NON_NULL)`이 null 필드를 제거하므로, 단순 성공은 `{ "success": true, "data": {...} }`.

- 컨트롤러 **클래스 또는 메서드**에 `@WrapResponse`를 붙인다. `ResponseWrapperAdvice`가
  원시 반환값을 자동으로 감싸므로 — 컨트롤러 메서드는 **도메인 객체를 직접 반환**한다
  (예: `return userService.getUserById(...)`). 직접 `ApiResponse`를 만들어 반환하지 않는다.
- 이미 `ApiResponse`나 `ResponseEntity`를 반환하는 메서드는 advice가 건드리지 않는다.
- `String` 반환은 특수 처리된다: advice가 래퍼를 수동으로 JSON 직렬화하므로
  (`ResponseWrapperAdvice` 참조) `@WrapResponse` 메서드에서 `String`을 반환해도 안전하다.

**신규 컨트롤러에는 반드시 `@WrapResponse`를 붙인다** (클래스 레벨이 기존 스타일).

## 4. 예외 처리

`GlobalExceptionHandler`(`@RestControllerAdvice`)가 예외를 `ApiResponse.error(...)`로 매핑한다:

| 예외 | HTTP 상태 |
|------|-----------|
| `MethodArgumentNotValidException` (빈 검증) | 400 + 필드 에러 맵 |
| `MethodArgumentTypeMismatchException` | 400 |
| `IllegalArgumentException` | 400 |
| `ResourceNotFoundException` | 404 |
| `Exception` (fallback) | 500 |

**컨벤션:** 서비스는 문서가 없을 때 `ResourceNotFoundException(resource, field, value)`를 던진다
(`UserService` 참조). 기존 상태 매핑으로 부족할 때만 `exception/` 아래에 새 커스텀 예외와
대응하는 `@ExceptionHandler`를 추가한다. catch 후 무시(swallow)하지 않는다.

## 5. 영속성 (MongoDB + ObjectId)

- 문서는 `@Document(collection = "...")` POJO이며 `@Id ObjectId id`를 가진다.
- 리포지토리는 인터페이스: `interface XRepository extends MongoRepository<X, ObjectId>`.
- ID는 네트워크 상에서 **hex 문자열**로 오간다. 컨트롤러 경계에서 `new ObjectId(id)`로 변환한다.
  잘못된 hex 문자열은 `IllegalArgumentException`을 던져 자동으로 400 처리된다.
- 인덱스는 `@Indexed` / `@CompoundIndex` 사용 (`UserSwipe`의 unique `userId+swipeId` 참조).
- 타임스탬프: 생성자에서 `createdAt`/`updatedAt`을 `LocalDateTime.now()`로 설정하고, 변경 시
  `updatedAt`을 갱신한다. ISO 문자열로 직렬화됨 (jsr310 + `WRITE_DATES_AS_TIMESTAMPS` 비활성).

## 6. 도메인 모델

"Buy it or not" — 사용자가 게임을 스와이프한다 (buy / skip / maybe).

| 컬렉션 | 주요 필드 | 비고 |
|--------|----------|------|
| `users` (`User`) | `nickname`, `createdAt` | `User(nickname)` 생성자 |
| `games` (`Game`) | `imageUrl`, `genre`, `releaseYear`, `title`, 타임스탬프 | |
| `swipes` (`Swipe`) | `gameIdList: List<ObjectId>`, `createdAt` | 게임 덱(deck) |
| `user_swipes` (`UserSwipe`) | `userId`, `swipeId`, `buy/skip/maybe: List<ObjectId>`, 타임스탬프 | unique `(userId, swipeId)` |

현재 `UserRepository`만 존재한다. `Game/Swipe/UserSwipe` 리포지토리는 아직 없으므로 —
기능이 필요할 때 추가한다.

## 7. 네이밍 & 스타일 컨벤션

- camelCase JSON 필드 (Jackson 기본값; snake_case는 어디에도 쓰지 않음).
- 문서에 Lombok `@FieldDefaults(level = PRIVATE)` → 필드를 명시적 `private` 없이 선언.
- `@RequiredArgsConstructor` + `final` 필드로 생성자 주입 (필드 `@Autowired` 금지).
- `@RequestMapping("/resource")`를 클래스 레벨에, HTTP 메서드 매핑을 메서드 레벨에 둔다.
- 기존 엔드포인트: `GET /users/{id}`, `GET /swipe/test`, `GET /ai/test?question=`.

## 8. 빌드 & 실행 명령어

```bash
./gradlew compileJava        # 빠른 컴파일 체크 (테스트 제외)
./gradlew test               # JUnit 플랫폼으로 전체 테스트 실행
./gradlew test --tests 'com.willu.buyitornot.web.ui.UserControllerTest'   # 단일 클래스
./gradlew build              # 테스트 포함 전체 빌드
./gradlew bootRun            # 앱 실행 (localhost:27017 MongoDB 필요)
```

Gradle 래퍼가 커밋되어 있다(`./gradlew`). 전역 `gradle`이 있다고 가정하지 않는다.

## 9. 테스트 컨벤션

- JUnit 5 (`org.junit.jupiter.api.Test`). 테스트 패키지는 `src/test/java` 아래에서 소스 패키지를 미러링.
- 가능하면 전체 컨텍스트보다 슬라이스 테스트를 선호:
  - `@WebMvcTest(XController.class)` + `MockMvc` + `@MockBean` 서비스 — 컨트롤러/HTTP shape 테스트.
  - `@DataMongoTest` — 리포지토리/쿼리 테스트 (Mongo 필요; 아래 폴백 참조).
  - `@SpringBootTest` — 와이어링/통합 전용 (`contextLoads`가 이미 존재).
- MockMvc 테스트에서는 **래퍼 shape**를 검증한다: `$.success`, `$.data.*`, `$.error` — 실제
  클라이언트는 감싸진 응답을 보지, 원시 도메인 객체를 보지 않기 때문이다.
- Mongo를 쓸 수 없는 CI/샌드박스라면? 모킹된 서비스를 쓰는 `@WebMvcTest`를 우선하거나,
  embedded-mongo를 추가한다. 환경 부재를 미검증 엔드포인트로 두지 말고 — 명시적으로 기록한다.
