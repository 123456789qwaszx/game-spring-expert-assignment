# WebCraft 숙련 프로젝트 PLAN — Lv1 ~ Lv15

## 0. 목표

`game-spring-expert-assignment`의 필수 과제 Lv1~Lv15를 순서대로 구현하고,
각 단계마다 **코드 작성 → 제공 테스트 → 실제 실행 확인 → 커밋**까지 완료한다.

이번 프로젝트의 핵심은 다음 세 종류의 상태를 구분하고 연결하는 것이다.

```text
MySQL
= 서버 재시작 후에도 남아야 하는 영속 데이터

JVM Memory
= 현재 서버 프로세스가 직접 가지고 있는 WebSocket 연결

Redis
= 빠르게 공유하고 만료시킬 실시간/임시 상태
```

최종적으로 아래 흐름이 동작해야 한다.

```text
REST
Controller
  ↓
Service
  ↓
Repository
  ↓
MySQL

WebSocket
Handshake
  ↓
Session Attributes
  ↓
WorldSessionRegistry
  ↓
MessageRouter
  ↓
Handler
  ├─ Ping → Redis Presence
  ├─ Move → Engine Queue
  ├─ Chat → MySQL → Broadcast
  └─ OnlineUsers → 현재 Session 조회
```

---

## 1. 공통 작업 원칙

### 1.1 기존 구조를 먼저 읽는다

각 Lv를 시작할 때 아래 순서로 확인한다.

```text
발제 요구사항
→ API 명세
→ TODO가 있는 클래스
→ 주변 제공 코드
→ 해당 테스트
→ 구현
```

TODO만 보고 주변 제공 코드를 임의로 다시 설계하지 않는다.

### 1.2 수정 범위를 최소화한다

- 기존 클래스 이름과 패키지를 변경하지 않는다.
- 엔티티 연관관계는 단방향만 사용한다.
- 제공 엔진 코드와 이미 완성된 생명주기 로직은 가급적 수정하지 않는다.
- 요구사항 해결을 위해 필요한 TODO와 직접 연결된 코드만 변경한다.

### 1.3 완료 기준

각 Level은 다음 네 단계가 모두 끝나야 완료로 판정한다.

```text
[ ] 요구사항대로 구현
[ ] 제공 테스트 통과
[ ] 실제 서버/API/게임에서 동작 확인
[ ] 의미 있는 커밋 생성
```

테스트 코드만 통과하거나 코드를 작성한 것만으로 완료 처리하지 않는다.

### 1.4 API 명세 우선

경로, HTTP 메서드, JSON 필드, enum 값, 상태 코드는 반드시 제공 API 문서를 기준으로 맞춘다.

- API 문서: `https://f-api.github.io/game-spring-api-docs/expert/api-docs.html`
- 완성 게임: `https://f-api.github.io/game-spring-api-docs/expert/`

### 1.5 실행 환경도 코드로 남긴다

이전 프로젝트 피드백을 반영해 MySQL/Redis 실행 환경은 가능한 한 `docker-compose.yml`로 재현 가능하게 남긴다.

목표 실행 경험:

```text
git clone
→ docker compose up -d
→ Spring 실행
```

---

# 2. Milestone 구성

| Milestone | 대응 Lv | 주제 | 완료 결과 |
|---|---:|---|---|
| M0 | Lv1 | 실행 기반 | MySQL + Redis + Spring 연결 |
| M1 | Lv2 | JPA Index | 채팅 복합 인덱스 생성 및 시작 검사 통과 |
| M2 | Lv3 | Player REST | 플레이어 등록 + Validation + 중복 처리 |
| M3 | Lv4 | World 생성 | 동시 생성 보호 + 월드 3개 제한 |
| M4 | Lv5~6 | Chat REST | 채팅 저장 + 최근 채팅 조회 API |
| M5 | Lv7~9 | WebSocket 연결 | Handshake + Interceptor 등록 + Session Registry |
| M6 | Lv10~11 | Presence / Router | Redis Presence + Ping/Pong + MessageRouter |
| M7 | Lv12~14 | 실시간 게임/채팅 | Move + Chat 저장/응답 + World Broadcast |
| M8 | Lv15 | 접속자/통합 | OnlineUsers + Lv1~15 전체 회귀 검증 |

---

# 3. M0 — Lv1 Docker로 MySQL과 Redis 설정

## 목표

개발에 필요한 MySQL과 Redis를 Docker로 실행하고 Spring 애플리케이션에서 연결한다.

## 핵심 학습

- Docker container
- port mapping
- MySQL / Redis 역할 차이
- Spring datasource / Redis 설정
- `ddl-auto=update`
- 재현 가능한 실행환경

## 주요 작업

- Docker 상태 확인
- MySQL 컨테이너 실행
- Redis 컨테이너 실행
- 가능하면 `docker-compose.yml` 작성
- `application.properties` 연결 설정
- Spring 부팅 확인
- MySQL/Redis 실제 접속 확인

## 완료 기준

```text
[ ] MySQL 실행
[ ] Redis 실행
[ ] Spring이 두 저장소에 정상 연결
[ ] ddl-auto=update 설정
[ ] docker ps 확인
[ ] 서버 부팅 성공
[ ] Lv1 커밋
```

상세: `M0.md`

---

# 4. M1 — Lv2 SQL을 JPA Index로 표현

## 목표

`chat_messages(world_id, created_at)` 복합 인덱스를 JPA 매핑으로 선언하고 실제 MySQL에 생성한다.

## 핵심 학습

- Index의 목적
- 복합 인덱스
- 컬럼 순서
- `@Table(indexes=...)`
- `@Index(name, columnList)`
- Entity mapping과 실제 DB schema의 관계

## 주요 작업

- `ChatMessage` 구조 확인
- `idx_chat_world_created_at` 선언
- 서버 실행
- MySQL에서 실제 index 확인
- 시작 검사 `CHAT_HISTORY_INDEX_MISSING` 제거 확인

## 완료 기준

```text
[ ] JPA annotation으로 인덱스 선언
[ ] 이름 정확
[ ] 컬럼 순서 정확
[ ] 실제 DB 생성 확인
[ ] 서버 시작 검사 통과
[ ] Lv2 커밋
```

상세: `M1.md`

---

# 5. M2 — Lv3 플레이어 등록

## 목표

API 명세에 맞는 플레이어 등록 REST API를 완성한다.

## 핵심 학습

- Request DTO
- Bean Validation
- `@Valid`
- `@RequestBody`
- Controller / Service / Repository
- DB Unique Constraint
- Conflict 처리

## 주요 작업

- `CreatePlayerRequest` 검증
- `PlayerController` API 매핑
- `PlayerService` 중복 검사
- 제공 `savePlayer()` 사용
- 201 + body 없음
- `DUPLICATE_NICKNAME` 확인

## 테스트

- `PlayerRegistrationTest`
- `PlayerApiTest`

상세: `M2.md`

---

# 6. M3 — Lv4 월드 생성

## 목표

월드 생성 요청을 직렬화하고 기본 월드 최대 3개 제한을 적용한다.

## 핵심 학습

- 동시성
- critical section
- 람다
- 서비스 트랜잭션
- count 후 create의 경쟁 조건

## 주요 작업

- `worldOperations.duringCreation()` 사용
- 람다 내부 `countRootWorlds()`
- `MAX_WORLDS` 검사
- 초과 시 `WORLD_LIMIT_REACHED`
- 허용 시 `createPreparedWorld(request)`

## 테스트

- `WorldCreationTest`

상세: `M3.md`

---

# 7. M4 — Lv5~6 채팅 저장과 최근 조회 REST

## 목표

채팅을 MySQL에 저장하고 최근 채팅을 REST API로 조회한다.

## 핵심 학습

- Entity 저장
- 사용자 식별 정보와 요청 payload의 경계
- 정렬 조회
- PageRequest
- Response DTO
- REST path/query parameter

## 주요 작업

### Lv5

- `ChatService.saveMessage()`
- `chatMessageRepository.save()`
- `savedResponse(worldId, saved)`
- 최근 메시지 조회
- 최신순 DB 결과를 API 요구 순서로 변환

### Lv6

- `WorldChatController`
- GET 경로
- `{worldId}`
- `limit` 기본값
- `RecentChatQueryService.getRecentMessages()`
- `ResponseEntity`

## 테스트

- `ChatServiceTest`
- `RecentChatApiTest`

상세: `M4.md`

---

# 8. M5 — Lv7~9 WebSocket 연결과 세션

## 목표

WebSocket handshake에서 사용자를 검증하고, 연결 이후 사용할 Session 정보를 저장하며,
월드별 실제 WebSocket 연결을 JVM 메모리에서 관리한다.

## 핵심 학습

- HTTP → WebSocket Upgrade
- HandshakeInterceptor
- Session Attributes
- WebSocketConfig
- WebSocketSession
- ConcurrentHashMap
- `putIfAbsent`

## 주요 작업

### Lv7

- Player 조회
- World 조회
- nickname/worldId session attribute 저장

### Lv8

- `NicknameHandshakeInterceptor`를 `/ws/worlds/{worldId}`에 등록

### Lv9

- `WorldSessionRegistry.register()`
- `putIfAbsent`
- `WorldSessionRegistry.get()`
- 같은 월드 중복 닉네임 보호

## 테스트

- `NicknameHandshakeInterceptorTest`
- `WebSocketConfigTest`
- `WorldSessionRegistryTest`

상세: `M5.md`

---

# 9. M6 — Lv10~11 Redis Presence와 Ping/Pong

## 목표

실제 WebSocket 객체와 접속 생존 상태를 분리하고,
Redis Presence를 Ping/Pong 메시지 흐름과 연결한다.

## 핵심 학습

- Redis Sorted Set
- member / score
- TTL
- heartbeat
- Router → Handler
- Ping/Pong

## 주요 작업

### Lv10

- `PresenceService.join()`
- ZSet add
- `connectionId` member
- `expiresAt()` score
- `PresenceService.leave()`
- ZSet remove

### Lv11

- `MessageRouter.route()` → `handler.handle(context, message)`
- `PingWsHandler`
- `presenceService.heartbeat(...)`
- `PongResponse`
- 요청한 session에만 응답

## 테스트

- `PresenceServiceTest`
- `MessageRouterTest`
- `PingWsHandlerTest`

상세: `M6.md`

---

# 10. M7 — Lv12~14 Move와 실시간 Chat

## 목표

실제 게임의 이동 요청을 엔진 큐에 전달하고,
채팅을 저장한 뒤 같은 월드의 모든 연결에 전달한다.

## 핵심 학습

- WS payload parsing
- Connection Context
- 신뢰 경계
- Engine Queue
- Chat persistence
- DTO
- Event/Delivery
- Broadcast

## 주요 작업

### Lv12 Move

- `WsFields`로 값 읽기
- nickname/worldId는 request body가 아니라 연결 Context 사용
- `PlayerAction.Move`
- `engineManager.enqueue()`

### Lv13 Chat

- `readContent()`
- `ChatService` 호출
- `ChatResponse` 필드/생성자
- DB 저장 확인

### Lv14 Broadcast

- `LocalChatSender.send()`
- `WorldBroadcaster.broadcast(worldId, message)`
- 보낸 사람 포함
- 다른 월드 제외

## 테스트

- `MoveWsHandlerTest`
- `ChatWsHandlerTest`
- `LocalChatSenderTest`

상세: `M7.md`

---

# 11. M8 — Lv15 접속자 목록과 필수 전체 통합

## 목표

현재 월드의 실제 열린 WebSocket 연결을 기준으로 접속자 목록을 반환하고,
Lv1~15의 전체 서버 흐름을 회귀 검증한다.

## 핵심 학습

- Registry와 Presence의 역할 차이
- Session attribute
- open session filtering
- 정렬
- 단일 session 응답
- 통합 검증

## 주요 작업

### Lv15

- `registry.entries(worldId)`
- 열린 session만 선택
- nickname attribute 읽기
- 명세 기준 정렬
- `users`
- `count`
- 요청 session에만 응답

### 전체 회귀

```text
Player 등록
→ World 생성
→ WebSocket 연결
→ Presence 등록
→ Ping/Pong
→ Move
→ Chat 저장
→ 같은 World Broadcast
→ Recent Chat REST
→ Online Users
```

## 테스트

- `OnlineUsersWsHandlerTest`
- Lv1~15에서 해제한 전체 테스트 재실행
- 실제 게임 + Postman/WebSocket 통합 확인

상세: `M8.md`

---

# 12. Commit 계획

Milestone마다 최소 하나 이상의 의미 있는 커밋을 만든다.

예시:

```text
chore: configure MySQL and Redis with Docker Compose
feat: add chat history database index
feat: implement player registration
feat: enforce world creation limit
feat: implement chat persistence and recent chat API
feat: implement WebSocket handshake and session registry
feat: implement Redis presence and ping pong routing
feat: implement move and realtime chat handlers
feat: implement online users and verify required flows
```

Lv 단위로 분리하는 것이 더 자연스러우면 M 내부에서도 여러 번 커밋한다.

---

# 13. README / TIL 병행 계획

필수 구현이 끝난 후 한 번에 작성하지 않는다.

| 시점 | 기록 |
|---|---|
| M0 | 실행 방법, Docker 환경 |
| M1 | Index 설명 |
| M2~M4 | REST API |
| M4 | DB/ERD 초안 |
| M5 | WebSocket 연결 구조 |
| M6 | Redis Presence와 Ping/Pong |
| M7 | Move/Chat 실시간 흐름 |
| M8 | ERD 최종, API 명세, 실행 화면, 트러블슈팅 |

TIL에는 적어도 하나 이상의 실제 문제를 아래 구조로 기록한다.

```text
문제
→ 관찰
→ 원인 가설
→ 확인 방법
→ 수정
→ 재검증
→ 배운 점
```

---

# 14. Lv1~15 최종 완료 체크

## 환경

- [ ] MySQL Docker 실행
- [ ] Redis Docker 실행
- [ ] Spring 연결
- [ ] 실행 환경 재현 가능

## DB / REST

- [ ] Chat 복합 Index
- [ ] Player 등록
- [ ] World 생성
- [ ] Chat 저장
- [ ] Recent Chat API

## WebSocket

- [ ] Handshake 사용자 식별
- [ ] Interceptor 등록
- [ ] 월드별 Session Registry
- [ ] Redis Presence
- [ ] Message Router
- [ ] Ping/Pong
- [ ] Move
- [ ] Chat WS
- [ ] World Broadcast
- [ ] Online Users

## 검증

- [ ] 각 Lv 제공 테스트 통과
- [ ] 실제 게임 동작 확인
- [ ] API 명세와 상태 코드 확인
- [ ] 전체 regression 통과
- [ ] README API 명세
- [ ] README ERD
- [ ] TIL 작성
- [ ] 제출용 GitHub 상태 확인

---

# 15. Lv15 이후

Lv1~15 필수 기능이 완전히 닫힌 뒤에만 다음 도전 기능으로 이동한다.

```text
Lv16 Optimistic Lock
Lv17 Cursor Pagination
Lv18 Redis Recent Chat Cache
Lv19 Redis Lua Rate Limit
Lv20 Multi Server
```

필수 기능의 회귀 테스트가 깨진 상태에서 도전 기능을 먼저 진행하지 않는다.
