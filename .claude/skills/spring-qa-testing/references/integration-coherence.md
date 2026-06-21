# 통합 정합성 검증 — Spring REST / MongoDB 판

경계면 버그는 "각 컴포넌트는 올바른데 연결 지점에서 계약이 어긋나는" 결함이다. 단위 테스트 통과나
컴파일 성공으로는 못 잡는다. QA는 반드시 **양쪽을 동시에 읽어** 교차 비교한다. 이 문서는 이
프로젝트(Spring Boot + MongoDB + `@WrapResponse` 래퍼)에서 점검할 경계면을 정의한다.

## 목차
1. 경계면 불일치 패턴
2. 교차 검증 영역
3. 통합 정합성 체크리스트

---

## 1. 경계면 불일치 패턴 (이 프로젝트에서 발생 가능)

| 경계면 | 불일치 예시 | 놓치는 이유 |
|--------|-----------|-----------|
| 컨트롤러 반환 → 래핑된 응답 | 메서드가 `ApiResponse`를 직접 반환 + `@WrapResponse` → 이중 래핑 `{data:{success,data}}` | 단건 단언만 하면 중첩을 못 봄 |
| 응답 shape → 테스트 단언 | 실제 `{success,data:User}`인데 테스트가 `$.nickname`(래퍼 무시) 단언 | 도메인 필드만 보고 래퍼를 건너뜀 |
| ObjectId(내부) → JSON(외부) | 내부 `ObjectId`가 `{ "timestamp":..., "date":... }`로 직렬화되거나 hex로 직렬화 | 직렬화 형태를 실제 응답으로 확인 안 함 |
| 잘못된 입력 → 상태코드 | 잘못된 hex가 500으로 새어나감(기대는 400) | 예외→핸들러 매핑을 실제 요청으로 검증 안 함 |
| `@WebMvcTest` 슬라이스 → advice/config | 슬라이스에 `ObjectMapper`/advice 미로드 → 래퍼·날짜 형식이 런타임과 다름 | 슬라이스 한계를 모르고 통과를 신뢰 |
| unique 인덱스(맵) → 저장 코드 | `UserSwipe`의 `(userId,swipeId)` unique인데 중복 저장이 예외 없이 통과 | 제약 정의만 보고 실제 저장 시도 안 함 |
| 엔드포인트 존재 → 실제 호출 가능 | 컨트롤러는 있는데 경로/메서드가 설계와 불일치 | 경로 문자열을 설계와 1:1 대조 안 함 |

## 2. 왜 정적 리뷰/컴파일로 못 잡나

- **컴파일 성공 ≠ 정상 동작.** 제네릭/`Object` 반환, advice 기반 래핑은 컴파일은 통과해도 런타임
  응답 shape이 달라질 수 있다.
- **슬라이스 테스트의 함정.** `@WebMvcTest`는 일부 빈만 로드한다. 커스텀 `ObjectMapper`(WebConfig)나
  advice가 빠지면 테스트가 통과해도 실제 서버 응답과 다르다. → 래퍼/날짜 단언이 의심되면
  `@Import(WebConfig.class)`로 보강하거나 `@SpringBootTest`로 교차 확인.
- **존재 검증 vs 연결 검증.** "엔드포인트가 있는가"와 "응답이 호출측 기대와 일치하는가"는 다른 검증이다.

## 3. 교차 검증 영역 (양쪽 동시 읽기)

각 영역에서 **생산자(왼쪽)** 와 **소비자(오른쪽)** 를 같이 열어 비교한다.

| 검증 대상 | 왼쪽 (생산자) | 오른쪽 (소비자) |
|----------|-------------|---------------|
| 응답 shape | 컨트롤러 반환 타입 + `@WrapResponse` + `ResponseWrapperAdvice` | 테스트의 `jsonPath("$.success"/"$.data...")` |
| 상태코드 | 서비스가 throw하는 예외 + `GlobalExceptionHandler` 매핑 | 테스트의 `status().isXxx()` |
| ID 직렬화 | `@Document`의 `ObjectId id` | 응답 JSON의 `$.data.id` 실제 형태 |
| 경로 계약 | `@RequestMapping`/`@GetMapping` 경로 | 설계 `01_design_*.md`의 엔드포인트 표 + 테스트 `perform(get("..."))` |
| 데이터 제약 | `@Indexed`/`@CompoundIndex(unique)` | 실제 저장 코드 + 중복 저장 시 동작 |
| 날짜 직렬화 | `LocalDateTime` 필드 + WebConfig(jsr310, no-timestamps) | 응답 JSON의 날짜 문자열 형태 |

### 응답 shape 교차 검증 절차
1. 컨트롤러 메서드 반환 타입을 확인한다(도메인 객체여야 정상; `ApiResponse` 직접 반환이면 이중 래핑 위험).
2. `@WrapResponse`가 클래스/메서드에 있는지 확인.
3. 실제 응답이 `{ success, data, message?, error? }`인지 MockMvc로 단언.
4. 래핑 여부 일치 확인: `data` 안에 도메인 필드가 있는가, 그 위에 `success`가 있는가.

### 상태코드 교차 검증 절차
1. 설계의 "검증 & 에러 흐름" 표에서 각 케이스의 기대 상태코드를 뽑는다.
2. 해당 케이스를 유발하는 입력으로 실제 요청을 보낸다(없는 id, 잘못된 hex, 빈 바디 등).
3. `GlobalExceptionHandler` 매핑과 실제 상태코드가 일치하는지 확인. 500으로 새면 미처리 예외 신호.

## 4. 통합 정합성 체크리스트

```markdown
### 응답 래퍼 정합성
- [ ] 컨트롤러는 도메인 객체를 직접 반환(ApiResponse 직접 생성 없음 → 이중 래핑 방지)
- [ ] @WrapResponse가 클래스 또는 메서드에 존재
- [ ] 실제 응답이 { success, data, ... } 형태임을 $.success / $.data 로 단언
- [ ] 에러 응답이 { success:false, error } 형태임을 단언
- [ ] (의심 시) @WebMvcTest 슬라이스에 WebConfig/advice가 로드됐는지 확인

### 상태코드 정합성
- [ ] 없는 리소스 → 404 (ResourceNotFoundException)
- [ ] 잘못된 ObjectId hex → 400 (IllegalArgumentException)
- [ ] 빈/무효 바디(검증) → 400 (MethodArgumentNotValidException) + 필드 에러 맵
- [ ] 미처리 예외가 500으로 새지 않는지(의도된 4xx인지) 확인

### 데이터 접근 정합성
- [ ] @Document 컬렉션명 ↔ 설계/기대 컬렉션 일치
- [ ] unique/복합 인덱스 제약이 실제 중복 저장에서 동작(가능 시 @DataMongoTest로 확인)
- [ ] ObjectId가 응답에서 기대한 형태(hex 문자열)로 직렬화
- [ ] LocalDateTime이 ISO 문자열로 직렬화(타임스탬프 숫자 아님)

### 경로/계약 정합성
- [ ] 컨트롤러 경로 ↔ 설계 엔드포인트 표 1:1 일치
- [ ] HTTP 메서드 ↔ 설계 일치
- [ ] 경로/쿼리/바디 파라미터 ↔ 설계 입력 명세 일치
```

> 이 체크리스트의 "존재 확인" 항목은 약하다. 항상 **교차 비교**(왼쪽 생산자 ↔ 오른쪽 소비자)를
> 우선한다. 한쪽만 읽고 통과시키지 않는다.
