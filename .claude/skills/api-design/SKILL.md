---
name: api-design
description: willubuyitornot-be(Spring Boot/MongoDB)에서 REST API 엔드포인트를 설계할 때 사용. 새 엔드포인트·API·기능 추가, 요청/응답 계약 정의, 검증 규칙·상태코드 설계, 데이터모델(@Document/리포지토리) 변경 분석이 필요하면 반드시 이 스킬을 사용한다. 코드 구현이 아니라 "무엇을 만들지" 설계 단계에 적용.
---

# API 설계 (willubuyitornot-be)

기능 요청을 이 프로젝트의 컨벤션에 맞는 **구현 가능한 엔드포인트 계약**으로 변환한다.
이 설계가 backend-engineer(구현)와 qa-engineer(검증)의 단일 진실 공급원이 된다.

> **먼저 읽어라:** `.claude/skills/backend-qa-orchestrator/references/project-conventions.md`
> (응답 래퍼·예외 매핑·ObjectId 경계·도메인 모델의 정식 정의). 이 스킬은 그 위에서 "설계 방법"만 다룬다.

## 설계 절차

1. **요청 해석** — 어떤 리소스에 대한 어떤 동작인가(조회/생성/수정/삭제/목록). 기존 도메인
   (User/Game/Swipe/UserSwipe) 중 무엇에 매핑되는가.
2. **엔드포인트 시그니처 결정** — 메서드 + 경로. REST 관례: 컬렉션 `/games`, 단건 `/games/{id}`.
   클래스 레벨 `@RequestMapping("/games")` + 메서드 레벨 매핑을 전제로 설계한다.
3. **입력 명세** — 경로 파라미터(ObjectId는 hex 문자열), 쿼리 파라미터, 요청 바디(JSON 필드 + 제약).
4. **응답 명세(래핑 기준)** — 클라이언트가 보는 것은 `{ success, data, message, error }` 래퍼다.
   **컨트롤러 반환 타입(도메인 객체)** 과 **래핑된 실제 응답 shape** 을 둘 다 적는다.
5. **검증 & 에러 흐름** — 각 입력의 유효/무효 케이스와 상태코드. 이게 곧 QA 테스트 케이스가 된다.
6. **데이터모델 영향** — 추가/변경할 `@Document`, 새 리포지토리, 인덱스/unique 제약.
7. **레이어 책임 분배** — 컨트롤러(얇게)/서비스(로직)/리포지토리(쿼리)에 무엇을 둘지.

## 설계 원칙

- **래퍼를 뚫고 생각하라.** 컨트롤러는 `@WrapResponse` 하에서 도메인 객체를 **직접 반환**한다.
  따라서 설계서의 "반환 타입"은 도메인 객체(`User`)이고, "클라이언트 응답"은 `{success,data:User}`다.
  이 둘을 혼동하면 QA 단언이 어긋난다.
- **상태코드는 예외 매핑에서 역산하라.** 없는 리소스→`ResourceNotFoundException`→404,
  잘못된 입력/ObjectId→`IllegalArgumentException`→400, 빈 검증→`MethodArgumentNotValidException`→400.
  새 상태코드가 필요하면 새 커스텀 예외 + 핸들러가 필요함을 설계서에 명시한다.
- **ObjectId 경계를 설계하라.** 외부는 hex 문자열, 내부는 `ObjectId`. 변환 지점(컨트롤러)과
  변환 실패 시 동작(400)을 항상 명시한다.
- **재사용 우선.** 새 서비스/리포지토리 전에 기존 것으로 가능한지 본다. 새 `@Document`는 정말
  새 컬렉션이 필요할 때만.
- **요청 범위만.** 추측성 엔드포인트를 끼워넣지 않는다. 작게 설계하고 필요 시 확장한다.

## 검증 규칙 설계 가이드

Bean Validation(`jakarta.validation`)을 전제로 한다. 요청 DTO 필드에 붙일 제약을 명세한다:

| 의도 | 애너테이션 | 위반 시 |
|------|-----------|--------|
| 필수 | `@NotNull` / `@NotBlank`(문자열) | 400 + 필드 에러 맵 |
| 길이 | `@Size(min=, max=)` | 400 |
| 숫자 범위 | `@Min` / `@Max` | 400 |

> DTO 검증을 쓰려면 컨트롤러 파라미터에 `@Valid`가 필요하고, `build.gradle`에
> `spring-boot-starter-validation` 의존성이 있어야 한다. 현재 없으므로, 검증이 필요한 설계라면
> "의존성 추가 필요"를 데이터모델 영향 섹션에 함께 적는다.

## 출력 형식

`_workspace/01_design_{feature}.md`에 저장한다. 구조는 api-designer 에이전트 정의의 출력 템플릿을
따른다(엔드포인트 / 요청 / 응답(래핑) / 검증&에러 흐름 / 데이터모델 영향 / 레이어 책임 /
QA 테스트 케이스 목록).

## 예시 (요청 → 설계 핵심)

요청: "닉네임으로 사용자를 생성하는 API"
- 엔드포인트: `POST /users`
- 요청 바디: `{ "nickname": "string, @NotBlank, @Size(max=20)" }` → DTO + `@Valid` 필요(검증 의존성 추가)
- 컨트롤러 반환: `User` (생성된 도메인 객체) → 클라이언트: `{ success:true, data:{ id, nickname, createdAt } }`
- 상태코드: 정상 201(또는 200), 빈 닉네임 400
- 데이터모델: 기존 `User`/`UserRepository` 재사용. `userRepository.save(new User(nickname))`.
- 레이어: Controller(바디 검증·서비스 위임) → Service(`createUser(nickname)`) → Repository(`save`).
