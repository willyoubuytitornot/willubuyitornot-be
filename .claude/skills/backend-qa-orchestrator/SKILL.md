---
name: backend-qa-orchestrator
description: willubuyitornot-be의 백엔드 API 개발 + QA 테스트를 조율하는 오케스트레이터. API/엔드포인트 추가·수정, 비즈니스 로직·데이터 접근 구현, 테스트 코드 작성·실행·검증 요청 시 반드시 이 스킬을 사용한다. 후속 작업도 포함 — "다시 실행", "재실행", "수정", "보완", "엔드포인트 추가", "테스트 추가/실행", "이전 결과 개선", "이 API만 다시" 등의 요청에도 반드시 이 스킬을 사용. 단순 코드 질문은 직접 응답 가능.
---

# Backend-QA Orchestrator (willubuyitornot-be)

백엔드 API 개발과 QA 테스트를 **생성-검증 에이전트 팀**으로 조율하는 통합 스킬.
설계 → 구현 → 점진 검증의 피드백 루프로, "동작이 검증된 코드"를 산출한다.

## 실행 모드: 에이전트 팀

설계자·구현자·QA가 실시간으로 협업한다. QA가 실패를 발견하면 구현자에게 직접 피드백하고
구현자가 수정하는 루프가 핵심이다. 이 상호작용이 품질을 만든다.

## 에이전트 구성

| 팀원 | 에이전트 타입 | 역할 | 스킬 | 출력 |
|------|-------------|------|------|------|
| api-designer | general-purpose | 엔드포인트 계약·검증·데이터모델 설계 | api-design | `_workspace/01_design_*.md` |
| backend-engineer | general-purpose | 컨트롤러/서비스/리포지토리/문서 구현 | spring-api-implementation | `src/main/...` + `_workspace/02_impl_*.md` |
| qa-engineer | general-purpose | 테스트 작성·실행·검증, 실패 피드백 | spring-qa-testing | `src/test/...` + `_workspace/03_qa_*.md` |

> 모델은 호출 시점의 세션 모델을 따른다(구독 플랜에 따라 Sonnet/Opus 등). Agent/TeamCreate 호출에
> `model` 파라미터를 강제하지 않는다.

## 워크플로우

### Phase 0: 컨텍스트 확인 (후속 작업 지원)

기존 산출물 존재 여부로 실행 모드를 결정한다:

1. 프로젝트 루트의 `_workspace/` 디렉토리 존재 여부 확인.
2. 결정:
   - **`_workspace/` 미존재** → 초기 실행. Phase 1로.
   - **`_workspace/` 존재 + 부분 수정 요청**("이 엔드포인트만 고쳐", "테스트만 다시") → **부분 재실행**.
     해당 에이전트만 재호출하고, 관련 산출물(`01/02/03_*`)만 갱신한다. 이전 산출물 경로를
     에이전트 프롬프트에 넣어 기존 결과를 읽고 반영하게 한다.
   - **`_workspace/` 존재 + 새 기능 요청** → **새 실행**. 기존 `_workspace/`를
     `_workspace_{YYYYMMDD_HHMMSS}/`로 이동(timestamp는 `date +%Y%m%d_%H%M%S`로 생성)한 뒤 Phase 1.

### Phase 1: 준비

1. 사용자 요청 분석 — 어떤 리소스(User/Game/Swipe/UserSwipe 등)의 어떤 동작인지, 새 기능인지 수정인지.
2. 컨벤션 숙지: `references/project-conventions.md`를 리더가 먼저 읽어 요청과 충돌이 없는지 본다.
3. `_workspace/` 준비(초기 실행 시 생성, 새 실행 시 위 규칙대로 보관 후 재생성).
4. 원본 요청을 `_workspace/00_request.md`에 저장.

### Phase 2: 팀 구성

1. 팀 생성:
   ```
   TeamCreate(
     team_name: "backend-qa-team",
     members: [
       { name: "api-designer",     agent_type: "general-purpose", prompt: "<api-designer.md 역할 + 이번 요청>" },
       { name: "backend-engineer", agent_type: "general-purpose", prompt: "<backend-engineer.md 역할 + 이번 요청>" },
       { name: "qa-engineer",      agent_type: "general-purpose", prompt: "<qa-engineer.md 역할 + 이번 요청>" }
     ]
   )
   ```
   각 prompt는 해당 `.claude/agents/{name}.md`의 역할·원칙·프로토콜을 담고, 이번 기능 요청과
   `_workspace/` 경로를 포함한다.

2. 작업 등록(의존성 명시):
   ```
   TaskCreate(tasks: [
     { title: "API 설계",        assignee: "api-designer" },
     { title: "엔드포인트 구현",   assignee: "backend-engineer", depends_on: ["API 설계"] },
     { title: "테스트 작성·실행",  assignee: "qa-engineer",      depends_on: ["엔드포인트 구현"] },
     { title: "실패 수정 루프",    assignee: "backend-engineer", depends_on: ["테스트 작성·실행"] }
   ])
   ```
   엔드포인트가 여러 개면 엔드포인트 단위로 구현→QA 작업을 쪼개 **점진 검증**되게 한다.

### Phase 3: 설계 → 구현 → 점진 검증

**실행 방식:** 팀원 자체 조율 + 리더 모니터링.

1. **설계** — api-designer가 `01_design_*.md` 작성 → 완료 시 backend-engineer에 SendMessage(설계 경로),
   qa-engineer에 테스트 케이스 목록 공유.
2. **구현** — backend-engineer가 설계대로 구현 → `./gradlew compileJava` 통과 확인 →
   `02_impl_*.md` 기록 → qa-engineer에 "구현 완료, 테스트 대상" 알림.
3. **검증** — qa-engineer가 테스트 작성 → `./gradlew test` 실행 → 통과/실패/미검증 판정 →
   `03_qa_*.md` 기록.
4. **피드백 루프** — 실패 시 qa-engineer → backend-engineer에 SendMessage(파일:라인 + 기대/실제 +
   수정 제안). 구현자 수정 → 재컴파일 → QA 재실행. 경계면 이슈는 api-designer에도 통지.
   같은 실패 2회 연속 시 리더에 에스컬레이션.

**팀원 간 통신 규칙:**
- api-designer → backend-engineer: 설계 파일 경로, 설계 변경점.
- backend-engineer → qa-engineer: 구현 완료 알림, 테스트 대상 파일 목록.
- qa-engineer → backend-engineer: 실패 리포트(구체적). → api-designer: 설계 모호성/경계면 이슈.

**리더 모니터링:** TaskGet으로 진행률 확인. 팀원이 막히면(idle) SendMessage로 개입.
QA가 실패 루프를 2회 넘기면 설계 재검토를 지시.

### Phase 4: 통합 검증 & 종합

1. 모든 작업 완료 대기(TaskGet).
2. 산출물 수집: `01_design_*.md`, `02_impl_*.md`, `03_qa_*.md`를 Read.
3. 최종 정합성 확인: QA 리포트의 "미검증 항목"이 남아있으면 사유와 함께 사용자 보고에 포함.
   실패 항목이 모두 해소됐는지, 컴파일·테스트가 초록인지 확인.
4. 사용자에게 요약: 추가/변경된 엔드포인트, 생성된 소스/테스트 파일, 테스트 통과 현황, 미검증 항목.

### Phase 5: 정리

1. 팀원에게 종료 SendMessage → TeamDelete.
2. `_workspace/`는 **보존**(중간 산출물 = 감사 추적). 삭제하지 않는다.
3. 사용자에게 결과 요약 + (선택) 피드백 요청: "팀 구성/워크플로우에 바꾸고 싶은 점이 있나요?"

## 데이터 흐름

```
[리더] → TeamCreate(3인) + TaskCreate
   │
api-designer ──01_design──▶ backend-engineer ──src + 02_impl──▶ qa-engineer
   ▲                            ▲                                  │
   │                            └────── 실패 리포트(SendMessage) ◀──┘
   └──── 경계면/설계 이슈 통지 ◀──────────────────────────────────┘
                                                                   │
                                              03_qa_report ────────┘
   리더: 01/02/03 Read → 정합성 확인 → 사용자 보고 → TeamDelete
```

## 에러 핸들링

| 상황 | 전략 |
|------|------|
| 컴파일 실패로 QA 진행 불가 | backend-engineer가 복구할 때까지 qa-engineer 대기. 리더가 TaskGet으로 감지 |
| 같은 테스트 2회 연속 실패 | 설계 결함 의심 → 리더가 api-designer에 설계 재검토 지시 |
| MongoDB 부재로 DB 테스트 불가 | `@WebMvcTest`로 가능 범위 검증, 미검증 항목을 리포트에 명시(누락 숨기지 않음) |
| 팀원 1명 실패/중지 | 리더가 idle 감지 → 상태 확인 → 재시작. 불가 시 작업 재할당 |
| 팀원 과반 실패 | 사용자에게 알리고 진행 여부 확인 |
| 설계↔구현 상충 | 삭제하지 말고 출처 병기, api-designer가 설계를 정정 후 재구현 |

## 테스트 시나리오

### 정상 흐름
1. 사용자: "닉네임으로 사용자 생성하는 POST /users 추가하고 테스트까지 해줘".
2. Phase 0: `_workspace/` 미존재 → 초기 실행.
3. Phase 1~2: `_workspace/` 생성, 3인 팀 + 4개 작업 등록.
4. Phase 3: api-designer가 `POST /users` 설계(201, 빈 닉네임 400) → backend-engineer가
   DTO+검증의존성+컨트롤러/서비스 구현, 컴파일 통과 → qa-engineer가 `@WebMvcTest` 작성,
   `./gradlew test` 실행, 래퍼 shape/상태코드 단언, 통과 확인.
5. Phase 4~5: 정합성 확인 후 "엔드포인트 1개 추가, 테스트 3개 통과" 보고, 팀 정리.
6. 예상 결과: 새 컨트롤러/서비스 + 테스트 클래스 생성, `./gradlew test` 초록.

### 에러 흐름
1. Phase 3에서 qa-engineer가 "빈 닉네임 → 기대 400, 실제 500" 실패 발견.
2. qa-engineer → backend-engineer SendMessage(파일:라인 + 원인: `@Valid` 누락 + 수정 제안).
3. backend-engineer가 `@Valid` 추가 + 검증 의존성 확인 → 재컴파일 → "수정 완료" 통지.
4. qa-engineer 재실행 → 400 확인 → 통과.
5. 만약 2회 연속 실패면 리더가 api-designer에 설계(검증 규칙) 재검토 지시.
6. 최종 리포트에 수정 이력과 최종 통과 현황 기록.

## 후속 작업 처리

Phase 0의 분기로 처리한다. "이 API만 다시", "테스트만 보완", "검증 추가" 같은 부분 요청은 해당
에이전트만 재호출하고 관련 `_workspace/` 산출물만 갱신한다. 각 에이전트는 정의의 "재호출 지침"에
따라 이전 산출물을 읽고 변경분만 반영한다(회귀 방지를 위해 기존 테스트는 유지·재실행).
