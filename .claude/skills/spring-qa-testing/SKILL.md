---
name: spring-qa-testing
description: willubuyitornot-be(Spring Boot/MongoDB)에서 QA·테스트를 수행할 때 사용. JUnit5/MockMvc/@WebMvcTest/@DataMongoTest/@SpringBootTest 테스트 코드 작성, ./gradlew test로 실제 실행, 응답 래퍼 shape 단언, 경계면 정합성 교차 검증, 동작 확인·회귀 검증이 필요하면 반드시 이 스킬을 사용한다. 테스트 작성/실행/검증/디버깅 모두 포함.
---

# Spring QA·테스트 (willubuyitornot-be)

구현이 설계대로 **실제로 동작하는지** 테스트로 검증한다. 핵심은 "존재 확인"이 아니라
"계약(경계면)이 지켜지는가"이며, 반드시 `./gradlew test`로 **실행**해 초록/빨강을 확인한다.

> **먼저 읽어라:** `.claude/skills/backend-qa-orchestrator/references/project-conventions.md`
> (테스트 컨벤션·응답 래퍼·예외 매핑). 경계면 교차 검증 상세는
> 이 스킬의 `references/integration-coherence.md` 참조.

## 테스트 유형 선택

| 검증 대상 | 테스트 유형 | Mongo 필요? |
|----------|-----------|------------|
| 컨트롤러 HTTP 동작·응답 shape·상태코드 | `@WebMvcTest(XController.class)` + `MockMvc` + `@MockBean` 서비스 | ❌ |
| 서비스 비즈니스 로직 | 순수 단위 테스트(Mockito로 리포지토리 목) 또는 `@SpringBootTest` | ❌(목) |
| 리포지토리 쿼리·인덱스/unique 제약 | `@DataMongoTest` | ✅ |
| 전체 와이어링/통합 | `@SpringBootTest` | ✅ |

**우선순위:** Mongo 없이 돌릴 수 있는 `@WebMvcTest`(컨트롤러) + Mockito(서비스)를 기본으로.
DB 의존 테스트는 Mongo 가용 시 또는 embedded-mongo 도입 시.

## @WebMvcTest 패턴 (가장 자주 쓰는 형태)

```java
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean UserService userService;

    @Test
    void getUser_존재하면_래핑된_200() throws Exception {
        User u = new User("홍길동");
        given(userService.getUserById(any())).willReturn(u);

        mockMvc.perform(get("/users/{id}", new ObjectId().toHexString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))      // ← 래퍼 shape 단언
            .andExpect(jsonPath("$.data.nickname").value("홍길동"));
    }

    @Test
    void getUser_없으면_404_에러래퍼() throws Exception {
        given(userService.getUserById(any()))
            .willThrow(new ResourceNotFoundException("User", "id", "x"));

        mockMvc.perform(get("/users/{id}", new ObjectId().toHexString()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void getUser_잘못된ObjectId_400() throws Exception {
        mockMvc.perform(get("/users/{id}", "not-a-valid-objectid"))
            .andExpect(status().isBadRequest());   // new ObjectId() → IllegalArgumentException → 400
    }
}
```

> **주의:** `@WebMvcTest`는 웹 레이어만 로드한다. `ResponseWrapperAdvice`와 `GlobalExceptionHandler`는
> `@RestControllerAdvice`라 슬라이스에 포함되지만, `WebConfig`의 커스텀 `ObjectMapper` 빈이 필요하면
> `@Import(WebConfig.class)`로 가져와야 할 수 있다. 래퍼/에러 응답 단언이 깨지면 advice·config가
> 로드됐는지 가장 먼저 확인한다.

## 실행

```bash
./gradlew test                                              # 전체
./gradlew test --tests 'com.willu.buyitornot.web.ui.UserControllerTest'   # 단일 클래스
./gradlew test --tests '*UserController*'                   # 패턴
```
리포트: `build/reports/tests/test/index.html`, 실패 상세는 콘솔 + `build/test-results/`.

## 검증 우선순위 (이 순서로 본다)

1. **경계면 정합성** (최우선) — 응답 래퍼 shape, ObjectId 직렬화, 상태코드↔예외 매핑.
   → `references/integration-coherence.md`의 체크리스트 적용.
2. **기능 스펙** — 설계 `01_design_*.md`의 "테스트 케이스 목록" 각 항목.
3. **데이터 접근** — 리포지토리 쿼리, unique/인덱스 제약이 실제 저장과 일치.
4. **회귀** — 기존 테스트(`contextLoads` 등) 유지·통과.

## 핵심 원칙

- **양쪽 동시 읽기.** 설계 ↔ 구현, 컨트롤러 반환 타입 ↔ 테스트 단언을 같이 열어 비교한다.
  한쪽만 보면 경계면 불일치를 놓친다.
- **래퍼 shape을 단언하라.** 도메인 필드만 단언하면 `@WrapResponse` 회귀를 못 잡는다.
  `$.success`/`$.data`/`$.error`를 반드시 확인한다.
- **실행으로 증명하라.** 통과 예상이 아니라 `./gradlew test` 결과로 판정한다.
- **점진 검증.** 모듈 완성 즉시 그 부분을 검증해 불일치 전파를 막는다.
- **환경 부재를 명시하라.** Mongo가 없어 DB 테스트를 못 돌리면 `@WebMvcTest`로 가능한 범위를 덮고,
  미검증 항목을 리포트에 적는다. 조용히 건너뛰지 않는다.

## 실패 피드백 형식 (→ backend-engineer)

실패는 **파일:라인 + 기대값 + 실제값 + 원인추정 + 수정제안**으로 전달한다.
구현 버그인지 테스트 버그인지 먼저 가린다(테스트 버그면 스스로 수정). 결과는
`_workspace/03_qa_{feature}.md`에 통과/실패/미검증을 구분해 기록한다.
