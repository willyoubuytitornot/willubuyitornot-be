---
name: spring-api-implementation
description: willubuyitornot-be(Spring Boot/MongoDB)에서 백엔드 API를 구현할 때 사용. 컨트롤러·서비스·리포지토리·@Document·예외 클래스를 작성/수정하거나, 설계 명세를 실제 Java 코드로 구현하거나, 비즈니스 로직·데이터 접근 코드를 만들 때 반드시 이 스킬을 사용한다. ./gradlew compileJava 컴파일 검증까지 포함.
---

# Spring API 구현 (willubuyitornot-be)

설계 명세를 이 프로젝트의 레이어 컨벤션에 맞는 동작하는 Java 코드로 구현한다.

> **먼저 읽어라:** `.claude/skills/backend-qa-orchestrator/references/project-conventions.md`
> (레이어 규칙·응답 래퍼·예외 매핑·Lombok 스타일·도메인 모델). 그리고 구현할 기능의
> `_workspace/01_design_*.md` 설계 파일.

## 레이어별 구현 패턴

정석 예시는 기존 `UserController` → `UserService` → `UserRepository`다. 그대로 복제한다.

### 1. @Document (영속성 모델) — `infra/collection/`
```java
@Document(collection = "games")
@Getter @Setter @NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Game {
    @Id ObjectId id;
    String title;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    public Game(String title) {
        this.title = title;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
```
- 타임스탬프는 생성자에서 `LocalDateTime.now()`. 변경 시 `updatedAt` 갱신.
- 인덱스는 `@Indexed` / 클래스에 `@CompoundIndex`.

### 2. Repository — `infra/repository/`
```java
@Repository
public interface GameRepository extends MongoRepository<Game, ObjectId> {
    // 쿼리 메서드는 메서드명 규칙으로: List<Game> findByGenre(String genre);
}
```

### 3. Service — `service/`
```java
@Service
@RequiredArgsConstructor
public class GameService {
    private final GameRepository gameRepository;

    public Game getGameById(ObjectId id) {
        return gameRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Game", "id", id.toHexString()));
    }
}
```
- 생성자 주입(`@RequiredArgsConstructor` + `final`). 필드 `@Autowired` 금지.
- 없는 리소스는 `ResourceNotFoundException(resource, field, value)` throw.

### 4. Controller — `web/ui/`
```java
@WrapResponse
@RestController
@RequestMapping("/games")
@RequiredArgsConstructor
public class GameController {
    private final GameService gameService;

    @GetMapping("/{id}")
    public Game getGame(@PathVariable String id) {
        return gameService.getGameById(new ObjectId(id));  // 도메인 객체 직접 반환
    }
}
```
- **클래스 레벨 `@WrapResponse`** 필수. 도메인 객체를 직접 반환(advice가 래핑).
- ObjectId 경계 변환은 컨트롤러에서 `new ObjectId(id)`. 잘못된 hex는 자동 400.
- 직접 `ApiResponse`를 만들지 않는다. `String` 반환도 안전.

### 5. 커스텀 예외 (필요 시) — `exception/`
기존 상태 매핑(400/404/500)으로 부족할 때만 새 예외 + `GlobalExceptionHandler`에 `@ExceptionHandler`
추가. 대부분은 `ResourceNotFoundException` / `IllegalArgumentException`으로 충분하다.

## 요청 바디 & 검증 구현

설계에 검증이 있으면:
1. 요청 DTO 클래스 작성(record 또는 Lombok POJO) + 필드에 `@NotBlank/@Size` 등.
2. 컨트롤러 파라미터에 `@Valid @RequestBody CreateXRequest req`.
3. `build.gradle`에 `spring-boot-starter-validation`이 없으면 추가해야 검증이 동작한다.
   (현재 미포함 — 설계가 검증을 요구하면 의존성 추가 후 구현)

## 구현 원칙

- **설계를 정확히 구현하라.** 설계와 다르게 가야 하면 임의로 바꾸지 말고 api-designer에게 알린다.
  설계↔구현 드리프트가 경계면 버그의 출발점이다.
- **얇은 컨트롤러.** 매핑·변환·위임만. 로직은 서비스로.
- **기존 코드 재사용.** 새 클래스 전에 기존 서비스/리포지토리로 가능한지 본다.
- **무관한 코드 불가침.** 요청 범위 밖 파일을 건드리지 않는다.

## 컴파일 검증 (인계 전 필수)

```bash
./gradlew compileJava        # 메인 소스 컴파일만 — 빠름
```
깨진 빌드를 QA에 넘기지 않는다. 실패하면 에러를 읽고 수정한 뒤 다시 컴파일한다.
인계 시 `_workspace/02_impl_{feature}.md`에 생성/수정 파일 목록과 컴파일 결과를 기록한다.
