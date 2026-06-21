# willubuyitornot-be

Spring Boot 3.3.5 / Java 21 / MongoDB 기반 REST API ("게임을 살까 말까" 스와이프 서비스).

## 커밋 규칙

- 커밋 메시지는 **한국어**로 작성한다.
- 커밋 메시지에 `Co-Authored-By` 트레일러를 **포함하지 않는다**.

## 하네스: 백엔드 API 개발 + QA 테스트

**목표:** 백엔드 API를 설계·구현하고, 테스트 코드로 실제 동작을 검증하는 생성-검증 에이전트 팀.

**트리거:** API/엔드포인트 추가·수정, 비즈니스 로직·데이터 접근 구현, 테스트 작성·실행·검증 관련
작업 요청 시 `backend-qa-orchestrator` 스킬을 사용하라. 후속 작업("다시 실행", "수정", "보완",
"엔드포인트 추가", "테스트 추가", "이 API만 다시")도 동일. 단순 코드 질문은 직접 응답 가능.

**프로젝트 컨벤션:** `.claude/skills/backend-qa-orchestrator/references/project-conventions.md`
(레이어 구조, `@WrapResponse`/`ApiResponse` 래퍼, 예외 매핑, ObjectId 경계, 빌드·테스트 명령어).

**변경 이력:**
| 날짜 | 변경 내용 | 대상 | 사유 |
|------|----------|------|------|
| 2026-06-21 | 초기 구성 (3인 팀: api-designer / backend-engineer / qa-engineer + 오케스트레이터) | 전체 | - |
