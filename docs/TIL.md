# 학습노트 — 직접 채우는 TIL

실행 결과는 M#-check에 남기고 여기에는 내 설명과 실제 경험을 쓴다.

## 발제 과제

---- Lv1 ----

[1] Docker로 MySQL과 Redis 실행 환경 구성

- MySQL과 Redis는 Spring 애플리케이션 내부가 아니라 별도의 프로세스로 동작한다.
- Docker Compose를 사용해 이번 프로젝트에서 사용할 MySQL과 Redis를 함께 실행하도록 구성했다.

사용 포트:

```text
MySQL: localhost:3309 -> container:3306
Redis: localhost:6379 -> container:6379
```

Docker Compose의 역할:

```text
docker-compose.yml
= MySQL / Redis를 어떤 설정으로 실행할지 정의
```

---

[2] MySQL 설정

MySQL 컨테이너에서 사용할 데이터베이스와 계정을 지정했다.

```text
Database: game_expert
User: game_user
```

Spring에서는 다음 주소로 접속한다.

```text
jdbc:mysql://localhost:3309/game_expert
```

흐름:

```text
Spring
→ localhost:3309
→ Docker Port Mapping
→ MySQL Container:3306
→ game_expert DB
```

---

[3] Redis 설정

Redis는 이후 WebSocket 접속 상태, 캐시 등의 데이터를 저장할 때 사용한다.

현재 Lv1에서는 Redis 서버가 정상적으로 실행되고 Spring이 접근할 수 있는 환경까지만 구성한다.

확인:

```bash
docker exec game-expert-redis redis-cli PING
```

결과:

```text
PONG
```

---

[4] application.properties 설정

Docker에서 실행한 MySQL과 Redis에 Spring이 접속할 수 있도록 설정했다.

```properties
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3309/game_expert}
spring.datasource.username=${DB_USERNAME:game_user}
spring.datasource.password=${DB_PASSWORD:game_password}

spring.jpa.hibernate.ddl-auto=update

spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
```

역할을 나누면:

```text
docker-compose.yml
= 외부 프로그램을 실행하는 설정

application.properties
= Spring이 외부 프로그램에 접속하는 설정
```

`${환경변수:기본값}` 형태를 사용하여 로컬에서는 기본값을 사용하고, 다른 환경에서는 환경변수로 교체할 수 있게 했다.

---

[5] ddl-auto=update

```properties
spring.jpa.hibernate.ddl-auto=update
```

JPA Entity 정보를 바탕으로 Hibernate가 기존 테이블을 유지하면서 필요한 테이블이나 컬럼을 갱신한다.

이번 과제에서는 이후 Level에서 Entity의 설정을 변경하면 그 설정이 실제 MySQL Schema에 반영되는 것을 확인할 수 있다.

예:

```text
Java Entity
→ JPA Metadata
→ Hibernate
→ DDL 실행
→ MySQL Schema
```

===

---- Lv2 ----

[1] 월드별 최근 채팅은 world_id로 범위를 좁힌 뒤 created_at 순서로 조회.

[2] 따라서 (world_id, created_at) 복합 인덱스를 사용.

[3] ddl-auto=update에 의해 JPA 선언이 실제 MySQL 인덱스로 반영된다.

===


---- Lv3 ----

[1] 플레이어 등록 흐름

POST /players
→ JSON을 CreatePlayerRequest로 변환
→ @Valid로 입력 검증
→ PlayerService
→ 중복 확인
→ Player 저장
→ 201 Created

[2] DTO Validation

@NotBlank
- null, 빈 문자열, 공백 차단

@Size(min = 2, max = 12)
- 닉네임 길이 제한

@Pattern(regexp = "^[a-zA-Z0-9_]+$")
- 영문 대소문자, 숫자, 밑줄만 허용

[3] @RequestBody와 @Valid

@RequestBody
- JSON을 DTO로 변환

@Valid
- Controller 본문 실행 전에 DTO의 Validation을 수행

검증 실패 시 Service는 호출되지 않고 400을 반환한다.

[4] 중복 닉네임

문자열 형식 오류가 아니라 현재 DB 상태와 충돌하는 문제이므로
ConflictException("DUPLICATE_NICKNAME")을 사용한다.

Service에서 existsByNickname()으로 먼저 검사하고,
DB의 UNIQUE 제약과 savePlayer()가 동시 요청에서도 최종적으로 중복 저장을 막는다.

[5] 성공 응답

플레이어 등록 성공:
201 Created
응답 Body 없음

===

---- Lv4 ----

[1] WorldService의 createWorld 완성 및 테스트 주석 해제

===

---- Lv5 ----

[1] 채팅 저장

worldId로 World 조회
→ 없으면 WORLD_NOT_FOUND
→ ChatMessage 생성
→ Repository 저장
→ ChatMessageResponse 반환


[2] 최근 채팅 조회

DB에서는 최근 N개를 찾기 위해

created_at DESC, id DESC

순서로 조회한다.

같은 created_at을 가진 메시지가 있을 수 있으므로
id를 두 번째 정렬 기준으로 사용한다.


[3] 반환 순서

DB 조회:
최신 → 과거

채팅 화면:
과거 → 최신

따라서 최근 N개를 조회한 뒤 List를 뒤집어서 반환한다.


[4] limit

최소 1
최대 100

Math.min(Math.max(limit, 1), MAX_LIMIT)

===