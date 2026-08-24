# kidhack-backend

자녀보호앱 REST API 서버 (Spring Boot / Java) — 인증, 부모-자녀 페어링, 위치·채팅·도청 등 핵심 도메인 로직

## 기술 스택

- Java 17
- Spring Boot
- Gradle (Groovy)

## 폴더 구조

기능(도메인)별로 패키지를 나누고, 각 도메인 패키지 안에 `controller / dto / entity / repository / service`를 동일한 구조로 둡니다.

```
src/main/java/com/example/kidhackbackend
├── admin/              # 관리자 도메인
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── repository
│   └── service
├── users/              # 사용자 도메인
│   ├── chat/           # 하위 기능은 도메인 폴더 안에 추가로 분리 (admin폴더 내부처럼) 
│   └── global/
├── global/              # 전역 공통 코드 (예외처리, 설정, 공통 유틸 등)
└── KidhackBackendApplication
```

새 기능을 추가할 때는 `com.example.kidhackbackend.{도메인명}` 패키지를 새로 만들고, 그 안에 `controller / dto / entity / repository / service` 패키지를 동일하게 구성해서 작업합니다. 하위 기능이 따로 있으면 `users/chat`처럼 도메인 폴더 아래에 서브 패키지로 나눕니다.

## 작업 흐름 (이슈 → 브랜치 → PR)

### 1) 이슈 생성

작업을 시작하기 전, GitHub Issues에 아래 형식으로 이슈를 먼저 등록합니다.

- 제목: `#이슈번호 기능이름` (예: `#1 게시판 CRUD`)
- 본문: 해당 기능에서 해야 할 일을 To-do 체크리스트로 작성

```markdown
## 할 일
- [ ] 게시글 등록 API
- [ ] 게시글 목록 조회 API
- [ ] 게시글 상세 조회 API
```

### 2) 브랜치 생성

이슈 번호를 기준으로 브랜치를 파서 작업합니다.

```
{작업유형}/{이슈번호}-{기능명}
ex. add/1-board_like, modify/1-board_like
```

예시:

```
add/1-board
fix/2-login-bug
refactor/3-user-service
```

```bash
git checkout main
git pull origin main
git checkout -b add/1-board
```

작업유형 예시: `add`(기능 추가), `fix`(버그 수정), `refactor`(리팩토링), `docs`(문서), `test`(테스트 코드)

### 3) 작업 및 커밋

자신이 맡은 도메인/파일만 수정하고, **수정한 파일만 골라서** add합니다. `git add .`처럼 전체를 한 번에 올리지 않습니다.

```bash
git add src/main/java/com/example/kidhackbackend/board/controller/BoardController.java
git add src/main/java/com/example/kidhackbackend/board/service/BoardService.java

git commit -m "add: 게시글 등록 API 구현"
```

커밋 메시지는 `작업유형: 작업 내용` 형식을 권장합니다. (예: `add: 게시글 목록 조회 API 추가`, `fix: 로그인 토큰 만료 오류 수정`)

### 4) 푸시 및 PR

```bash
git push origin add/1-board
```

GitHub에서 `add/1-board` → `main`으로 Pull Request를 생성하고, 관련 이슈 번호를 PR 본문에 `Closes #1`처럼 연결합니다. 리뷰 후 이상 없으면 main에 머지하고, 머지된 브랜치는 삭제합니다.


## 브랜치/이슈 규칙 요약

| 항목 | 규칙 |
| --- | --- |
| 이슈 제목 | `#이슈번호 기능이름` |
| 브랜치명 | `작업유형/이슈번호-기능명` |
| 커밋 메시지 | `작업유형: 작업 내용` |
| add 범위 | 자신이 수정한 파일만 |
| 머지 | PR 생성 후 main에 머지 |
