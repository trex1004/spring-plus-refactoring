# Spring Plus

기존 Todo API를 Spring Boot 4.1 환경으로 마이그레이션하고, 인증·인가, 트랜잭션, 연관관계, 조회 성능을 단계적으로 리팩터링한 프로젝트입니다.

> 이 문서는 현재 push된 Level 0~3 구현을 기준으로 작성했습니다. 작업 중인 실시간 채팅 기능은 검증과 커밋이 끝난 뒤 별도 섹션으로 추가합니다.

## 기술 스택

| 구분 | 기술 |
| --- | --- |
| Language | Java 17 |
| Framework | Spring Boot 4.1.1, Spring Security |
| Data | Spring Data JPA, QueryDSL 7.6, MySQL |
| Test | JUnit 5, Mockito, H2 |
| API 문서 | springdoc-openapi (Swagger UI) |
| Build | Gradle Wrapper 8.14.3 |

## 실행 방법

### 1. 사전 준비

- Java 17
- MySQL 8 이상
- `plus_spring` 데이터베이스

```sql
CREATE DATABASE plus_spring;
```

### 2. 환경변수 설정

```bash
export DB_USERNAME=사용자명
export DB_PASSWORD=비밀번호
export JWT_SECRET_KEY=32자_이상의_시크릿_키
```

### 3. 애플리케이션 실행

```bash
./gradlew bootRun
```

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

> 로컬 실행 환경은 Hibernate가 테이블을 생성하고 `data.sql`의 조회 검증 데이터를 초기화합니다.
> 기존 로컬 데이터 보존이 필요한 환경에서는 `ddl-auto`와 SQL 초기화 정책을 별도로 설정해야 합니다.

### 4. 테스트 실행

```bash
./gradlew clean test
```

테스트는 MySQL 대신 H2를 사용합니다. 실행 DB와 테스트 DB를 분리해 로컬 데이터에 영향을 주지 않도록 구성했습니다.

## API  확인

세부 요청·응답 형식은 Swagger UI에서 확인합니다.

| 분류      | 대표 API                                                          | 인증            |
| ------- | --------------------------------------------------------------- | ------------- |
| 인증      | `POST /auth/signup`, `POST /auth/signin`                        | 불필요           |
| Todo    | `POST /todos`, `GET /todos`, `GET /todos/{todoId}`              | 조회 불필요, 생성 필요 |
| Todo 검색 | `GET /todos/search`                                             | 불필요           |
| 댓글      | `POST /todos/{todoId}/comments`, `GET /todos/{todoId}/comments` | 조회 불필요, 생성 필요 |
| 담당자     | `POST /todos/{todoId}/managers`, `GET /todos/{todoId}/managers` | 조회 불필요, 등록 필요 |
| 사용자 역할  | `PATCH /admin/users/{userId}`                                   | ADMIN         |

## 구현 사항

### Level 0. 실행 환경 마이그레이션

| 항목 | 구현 |
| --- | --- |
| Framework | Spring Boot 4.1.1, Java 17, Gradle Wrapper 8.14.3 적용 |
| 라이브러리 호환 | JJWT 0.13 API와 Spring Boot 4 테스트 API로 변경 |
| 환경 분리 | MySQL 실행 환경과 H2 테스트 환경 분리 |

### Level 1. Todo·사용자 기본 흐름

| 항목 | 구현 |
| --- | --- |
| nickname과 JWT | nickname을 User·회원가입 응답·JWT Claim·`AuthUser`에 반영 |
| 쓰기 트랜잭션 | Todo 생성 로직에 트랜잭션을 적용해 변경 작업의 원자성 보장 |
| 선택 조건 조회 | JPQL로 날씨·수정일 범위를 선택 조건으로 받아 페이징 조회 |
| AOP 접근 로그 | 관리자 역할 변경 메서드 실행 전에 요청자·요청 URI·시각을 기록 |
| Controller 테스트 | 실제 예외 응답 계약에 맞춰 Todo 미존재 조회 테스트 수정 |

### Level 2. 연관관계·보안·조회 최적화

| 항목 | 구현 |
| --- | --- |
| 담당자 자동 등록 | Todo 생성 시 작성자를 `Manager`로 함께 저장하도록 `CascadeType.PERSIST` 적용 |
| 댓글 N+1 제거 | `@EntityGraph(attributePaths = "user")`로 댓글과 작성자를 함께 조회 |
| Todo 단건 N+1 제거 | QueryDSL fetch join으로 Todo와 작성자를 한 번에 조회 |
| JWT 인증·인가 전환 | `SecurityContext` 중심으로 인증 정보를 통일하고, Filter·SecurityConfig·Controller의 책임 분리 |

### Level 3. 도전 기능

| 항목 | 구현 |
| --- | --- |
| QueryDSL 검색 API | 제목·담당자 닉네임 부분 검색, 생성일 범위, 최신순 정렬, 페이징 |
| Projection | Todo 전체 대신 제목·담당자 수·댓글 수만 조회 |
| 독립 트랜잭션 로그 | `REQUIRES_NEW`로 담당자 등록 실패 후에도 요청 이력을 보존 |

## 핵심 설계

### 인증·인가 책임 분리

```text
JWT
 ↓
JwtFilter
 └─ JWT 검증 → AuthUser / Authentication 생성
 ↓
SecurityContext
 └─ 현재 인증 사용자 보관
 ↓
SecurityConfig
 └─ URL별 공개·인증·ADMIN 권한 정책 결정
 ↓
@Auth AuthUser
 └─ Controller에 principal 주입
```

- `JwtFilter`는 JWT 검증과 `Authentication` 생성만 담당합니다.
- URL별 접근 정책과 USER/ADMIN 권한 판정은 `SecurityConfig`가 담당합니다.
- 인증되지 않은 보호 API 요청은 401, 인증되었지만 ADMIN 권한이 없는 요청은 403으로 구분합니다.

### 담당자 등록과 요청 로그의 트랜잭션 분리

```text
ManagerService.saveManager()         ManagerAssignmentLogService.saveLog()
업무 트랜잭션                         REQUIRES_NEW 트랜잭션
        │                                      │
        └────────── 요청 로그 저장 ───────────→ COMMIT
        │
        └─ 검증 실패 → ROLLBACK
```

매니저 등록은 실패하면 롤백되어야 하지만, 누가 어떤 담당자 등록을 요청했는지는 남겨야 합니다. 로그 저장을 별도 Bean의 `REQUIRES_NEW` 트랜잭션으로 분리해 두 작업의 성공·실패 범위를 다르게 관리했습니다.

### QueryDSL 검색의 집계 방식

```text
Todo 1건
 ├─ 제목·생성일 조건: 외부 쿼리
 ├─ 담당자 닉네임 조건: EXISTS 서브쿼리
 ├─ 담당자 수: COUNT 서브쿼리
 └─ 댓글 수: COUNT 서브쿼리
```

`Manager`와 `Comment` 컬렉션을 본문 쿼리에서 동시에 join하면 행 수가 곱해져 집계가 왜곡될 수 있습니다.
 Todo를 기준으로 한 행을 유지하고, 검색 조건과 두 집계를 분리했습니다. `PageableExecutionUtils`로 전체
 count 쿼리는 필요한 경우에만 실행합니다.

## 검증

| 대상      | 확인 내용                                             |
| ------- | ------------------------------------------------- |
| 인증·인가   | 비인증 보호 API 401, USER의 관리자 API 403, ADMIN 요청 성공 확인 |
| 댓글 목록   | 댓글·작성자를 join 조회하는 SQL로 N+1 제거 확인                  |
| Todo 단건 | Todo·작성자를 fetch join하는 QueryDSL 쿼리 확인             |
| 검색 API  | 제목·닉네임·생성일 조건, 집계값, 페이징 및 날짜 범위 오류 확인             |
| 독립 트랜잭션 | 담당자 등록 실패 시 Manager 롤백과 요청 로그 저장을 통합 테스트로 확인      |

## 학습 기록

- [프로젝트 리팩터링 기록]([TIL_링크_추가](https://zeroto-dev.tistory.com/12))
- [JWT 인증 성공 로그가 있는데 관리자 API가 401이었던 이유]([TIL_링크_추가](https://zeroto-dev.tistory.com/11))

각 글에서는 구현 목록보다 설계 판단과 오류 해결 과정을 중심으로 정리했습니다.
