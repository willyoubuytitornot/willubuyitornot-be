# 이슈: 전체 스위트(`./gradlew test`) 실행 시 Testcontainers MongoDB 드라이버 hang

## 상태
미해결. 우회책 있음(개별 클래스 실행은 항상 정상).

## 증상
`./gradlew test`로 전체 테스트를 한 번에 돌리면 `UserControllerIntegrationTest`,
`SwipeControllerIntegrationTest`, `GameControllerIntegrationTest` 중 일부가 멈추거나
`MongoTimeoutException`으로 실패한다. 반면 각 클래스를 `--tests` 옵션으로 **개별 실행**하면
항상 정상 통과한다 (예: `./gradlew test --tests 'com.willu.buyitornot.web.ui.GameControllerIntegrationTest'`).

## 재현
```bash
TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock ./gradlew test
# → 특정 통합 테스트 클래스에서 멈추거나 MongoTimeoutException 발생
```

## 조사 경과 (2026-07-03)

1. **최초 증상**: 전체 스위트 실행 시 `UserControllerIntegrationTest`·`SwipeControllerIntegrationTest`
   7건씩 `MongoTimeoutException`으로 실패. 각 클래스 단독 실행은 정상.
2. **1차 가설(기각) — Colima 리소스 부족**: 로컬 Colima VM이 CPU 2코어/메모리 2GiB로 작게 설정돼
   있어 Gradle 데몬+테스트 JVM+MongoDB 컨테이너를 동시에 못 버틴다고 추정. `colima stop && colima
   start --cpu 4 --memory 4`로 증설했으나 **동일하게 재현**됨 (근본 원인 아님, 다만 증설 자체는
   무해하므로 유지함).
3. **2차 가설(기각) — Testcontainers 컨테이너 재사용 필요**: `AbstractMongoIntegrationTest`의
   `MongoDBContainer`에 `.withReuse(true)`를 추가하고 `~/.testcontainers.properties`에
   `testcontainers.reuse.enable=true`를 설정해봤으나, 오히려 **컨테이너가 실행 중간에 사라지고
   재생성되는 새로운 문제**가 발생(리소스 증설 전 실험). 리소스 증설 후에는 컨테이너 churn은
   사라졌지만 hang 자체는 reuse 유무와 무관하게 재현되어 **되돌림**.
4. **현재 확인된 사실**:
   - `AbstractMongoIntegrationTest`는 `@Container static final MongoDBContainer` 필드를
     **부모 클래스에 선언**하고 있어, 이를 상속하는 모든 통합 테스트 클래스가 **같은 JVM 실행 내에서
     동일한 컨테이너 인스턴스 하나를 공유**한다(Java의 static 필드는 선언 클래스 기준 하나만 존재).
   - hang 발생 시점에 `jstack`으로 스레드 덤프를 뜨면 테스트 스레드가
     `com.mongodb.internal.connection.BaseCluster.selectServer` → `CountDownLatch.await`에서
     수백 초간 멈춰 있다(`TIMED_WAITING`).
   - 같은 시점에 `docker port`로 확인한 매핑 포트에 호스트에서 `nc -zv 127.0.0.1 <port>`로
     접속하면 **즉시 성공**한다 — 즉 컨테이너 자체는 호스트에서 정상적으로 도달 가능한데,
     Java Mongo 드라이버만 서버를 못 찾고 멈춘다.
   - 따라서 **단순 리소스 부족이나 컨테이너 재사용 여부가 원인이 아니라, 여러
     `@SpringBootTest` 컨텍스트가 하나의 Testcontainers MongoDB 컨테이너를 순차 공유할 때
     드라이버 쪽 서버 디스커버리(SDAM)가 어떤 조건에서 멈추는 문제**로 보인다. Replica-set
     advertised host(컨테이너가 자기 자신을 어떤 호스트:포트로 광고하는지)가 두 번째 이후
     Spring 컨텍스트에서 드라이버가 참조하는 값과 어긋나는 시나리오가 유력한 후보이나,
     확정적으로 재현/격리하지는 못했다.

## 시도했으나 원인이 아니었던 것
- Colima 리소스 증설 (2CPU/2GiB → 4CPU/4GiB) — 무해하므로 유지, 하지만 hang을 고치지 못함.
- Testcontainers reuse (`.withReuse(true)` + `testcontainers.reuse.enable=true`) — 되돌림.
  오히려 컨테이너 churn이라는 별도 문제를 유발한 바 있음.

## 현재 우회책 (권장)
- CI/로컬 검증 시 전체 스위트(`./gradlew test`) 대신, 통합 테스트 클래스를 **개별적으로**
  `--tests` 옵션으로 순차 실행한다. 예:
  ```bash
  TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock \
    ./gradlew test --tests 'com.willu.buyitornot.web.ui.UserControllerIntegrationTest'
  TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock \
    ./gradlew test --tests 'com.willu.buyitornot.web.ui.SwipeControllerIntegrationTest'
  TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock \
    ./gradlew test --tests 'com.willu.buyitornot.web.ui.GameControllerIntegrationTest'
  ```
- `backend-qa-orchestrator`/`spring-qa-testing` 스킬로 QA를 돌릴 때도 전체 스위트를 반복
  재실행하지 말고, **변경된 기능의 테스트 클래스만 지정 실행**하는 것을 기본으로 삼는다
  (qa-engineer 에이전트가 전체 스위트 hang에 걸려 반복적으로 오래 대기한 전례가 있음 — 2026-07-03).

## 다음에 시도해볼 것 (미검증)
1. `AbstractMongoIntegrationTest`를 클래스마다 **독립된 컨테이너**를 갖도록 바꾸기
   (정적 필드를 부모가 아니라 각 서브클래스/또는 `@BeforeAll`에서 인스턴스 필드로 관리),
   공유로 인한 SDAM 꼬임 자체를 없앤다. 대신 클래스 수만큼 컨테이너가 뜨므로 리소스 사용은 늘어난다.
2. `MongoDBContainer` 생성 시 명시적으로 `.withSharding()`/replica-set 광고 호스트를
   `withNetworkAliases` 등으로 고정해 SDAM이 참조하는 호스트가 실제 매핑 포트와 항상 일치하도록
   강제.
3. Spring `@DirtiesContext`류로 클래스 간 컨텍스트를 강제로 새로 만들지 않고, 반대로
   완전히 하나의 공유 `@SpringBootTest` 컨텍스트(테스트 슈퍼클래스 통합)로 합쳐서 MongoClient
   빈 자체를 하나만 쓰게 만드는 구조도 검토 가능.
4. Testcontainers/Mongo 드라이버 버전 업그레이드 여부 확인 (현재 `mongo:7.0` 이미지 + 프로젝트에
   고정된 driver/testcontainers 버전 조합에서 알려진 이슈인지 GitHub issue 검색).

## 관련 커밋/변경 이력
- `src/test/java/com/willu/buyitornot/support/AbstractMongoIntegrationTest.java` —
  reuse 관련 실험은 커밋 없이 원복됨 (현재 저장소 상태는 실험 이전과 동일).
- Colima 로컬 설정(`~/.colima`, `~/.testcontainers.properties`)은 저장소 밖 개발자 로컬
  환경이라 커밋 대상 아님. `docs/mongodb-collections.md` 근처 컨벤션 문서
  (`.claude/skills/backend-qa-orchestrator/references/project-conventions.md` 9번 항목)에
  이미 Colima 관련 로컬 설정이 안내되어 있음 — 이번 이슈 내용도 필요 시 그쪽에 반영 검토.
