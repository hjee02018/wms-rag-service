# WMS RAG Service - 아키텍처 가이드

## 📋 프로젝트 개요

이 프로젝트는 **RAG (Retrieval-Augmented Generation)** 시스템을 구현한 Spring Boot 애플리케이션입니다.  
기존 **Spring-Vue 서비스**와 REST API 방식으로 통합되며, **Spring AI**를 활용하여 AI 기반 문서 검색 및 질의응답 기능을 제공합니다.

---

## 🏗️ 최종 프로젝트 구조

```
wms-rag-service/
├── src/main/java/com/github/hjyang/rag/wms_rag_service/
│
├── 📌 Application.java
│   └── 메인 Spring Boot 애플리케이션 진입점
│
├── 📁 config/                          ✨ [설정 레이어]
│   ├── AiConfig.java                  - OpenAI ChatClient, 임베딩 모델 설정
│   ├── CorsConfig.java                - CORS 설정 (Spring-Vue 서비스 연동용)
│   └── WebConfig.java                 - HTTP 메시지 컨버터 설정
│
├── 📁 controller/                      ✨ [API 레이어]
│   ├── RagController.java             - RAG 질의/응답 API (핵심 엔드포인트)
│   └── DocumentController.java        - 문서 관리 API (CRUD)
│
├── 📁 service/                         ✨ [비즈니스 로직 레이어]
│   ├── RagService.java                - RAG 핵심 로직 (검색 + 생성)
│   ├── DocumentService.java           - 문서 CRUD 및 인덱싱
│   └── VectorStoreService.java        - 벡터 스토어 관리 (임베딩, 유사도 검색)
│
├── 📁 repository/                      ✨ [데이터 접근 레이어]
│   └── DocumentRepository.java        - Document 엔티티 영속성 관리
│
├── 📁 model/                           ✨ [엔티티 / 도메인 모델]
│   └── Document.java                  - 문서 엔티티 (JPA)
│
├── 📁 dto/                             ✨ [데이터 전송 객체]
│   ├── RagQueryRequest.java           - RAG 질의 요청 DTO
│   ├── RagQueryResponse.java          - RAG 질의 응답 DTO
│   └── DocumentUploadRequest.java     - 문서 업로드 요청 DTO
│
├── 📁 exception/                       ✨ [예외 처리 레이어]
│   ├── RagException.java              - 커스텀 RAG 예외
│   └── GlobalExceptionHandler.java    - 전역 예외 처리기
│
├── resources/
│   ├── application.properties          - 애플리케이션 설정
│   └── ...
│
└── test/                               - 테스트 코드
```

---

## 🔄 시스템 아키텍처

### 전체 아키텍처

```
┌─────────────────────────────────────────────────────────────────────┐
│                     Spring-Vue Service (기존 서비스)                  │
│                                                                       │
│  Vue Frontend ◄──► Spring Controller ◄──► Business Logic             │
└──────────────────────────┬──────────────────────────────────────────┘
                           │
                           │ HTTP REST API 호출
                           │ (CORS 활성화)
                           ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    WMS RAG Service (이 프로젝트)                      │
│                                                                       │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │ 1️⃣  API 레이어 (REST Controller)                              │   │
│  │    ├── /api/rag/query       (POST/GET) - RAG 질의            │   │
│  │    ├── /api/rag/status      (GET)      - 상태 확인            │   │
│  │    └── /api/documents/*     (CRUD)     - 문서 관리             │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                           ▼                                           │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │ 2️⃣  비즈니스 로직 레이어 (Service)                              │   │
│  │    ┌─────────────────────────────────────────────────────┐    │   │
│  │    │ RagService (RAG 핵심 로직)                            │    │   │
│  │    │  • 질의 처리                                         │    │   │
│  │    │  • 문서 검색 조율                                    │    │   │
│  │    │  • 답변 생성                                         │    │   │
│  │    │  • 신뢰도 계산                                       │    │   │
│  │    └─────────────────────────────────────────────────────┘    │   │
│  │         │                  │                  │                │   │
│  │         ▼                  ▼                  ▼                │   │
│  │    ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │   │
│  │    │ Document     │  │ Vector       │  │ Integration  │      │   │
│  │    │ Service      │  │ Store        │  │ Logic        │      │   │
│  │    │ • CRUD       │  │ Service      │  │              │      │   │
│  │    │ • Indexing   │  │ • Embedding  │  │              │      │   │
│  │    │              │  │ • Search     │  │              │      │   │
│  │    └──────────────┘  └──────────────┘  └──────────────┘      │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                           ▼                                           │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │ 3️⃣  데이터 접근 레이어 (Repository & Persistence)               │   │
│  │    ├── DocumentRepository (JPA) ◄──► H2 Database (문서 저장)   │   │
│  │    └── VectorStore (Chroma) ◄──────► 벡터 임베딩 저장소        │   │
│  └──────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
                           │
                           ▼
                  ┌─────────────────────┐
                  │  OpenAI API         │
                  │  • 텍스트 임베딩    │
                  │  • 답변 생성        │
                  └─────────────────────┘
```

---

## 🔍 기능별 상세 구조

### 1️⃣ **문서 관리 기능** (Document Management)
```
DocumentController ─► DocumentService ─► DocumentRepository
    │                      │                    │
    ├─ POST /documents     ├─ createDocument()  └─► Document 테이블
    ├─ GET  /documents     ├─ getDocument()
    ├─ PUT  /documents/{id}├─ updateDocument()
    ├─ DELETE /documents/{id}├─ deleteDocument()
    └─ GET  /documents/search├─ searchByTitle()
                            │
                            └─► VectorStoreService
                                 (인덱싱)
```

### 2️⃣ **RAG 질의 처리 기능** (Query Processing)
```
요청: POST /api/rag/query
{
  "question": "어떻게 ...?",
  "topK": 5
}
        │
        ▼
RagController
        │
        ▼
RagService.query()
        │
        ├─► VectorStoreService.searchSimilarDocuments()
        │      │
        │      └─► Spring AI Vector Store
        │           (Chroma)
        │           • 질의 임베딩
        │           • 유사도 검색
        │
        ├─► buildContext() - 문서 조합
        │
        ├─► buildPrompt() - 프롬프트 생성
        │
        ├─► ChatClient.prompt().call()
        │      │
        │      └─► OpenAI API
        │           • 답변 생성
        │
        ▼
RagQueryResponse
{
  "answer": "...",
  "retrievedDocuments": [...],
  "confidence": 0.85,
  "processingTimeMs": 1234
}
```

### 3️⃣ **벡터 스토어 관리** (Vector Store Operations)
```
VectorStoreService
        │
        ├─ indexDocument()          ─► Chroma Vector Store
        │  • Spring AI Document 생성   • 임베딩 변환
        │  • 메타데이터 추가           • 벡터 저장
        │
        ├─ searchSimilarDocuments() ─► Chroma Vector Store
        │  • 질의 임베딩               • 유사도 검색
        │  • topK 지정                 • 관련 문서 반환
        │
        └─ deleteDocument()         ─► Chroma Vector Store
           • 벡터 삭제
```

---

## 📡 API 엔드포인트

### RAG 질의 API

#### 1. **POST /api/rag/query** - RAG 질의 (권장)
```bash
curl -X POST http://localhost:8080/api/rag/query \
  -H "Content-Type: application/json" \
  -d '{
    "question": "Spring AI란 무엇인가?",
    "topK": 5,
    "similarityThreshold": 0.3
  }'
```

**응답:**
```json
{
  "answer": "Spring AI는 ...",
  "retrievedDocuments": [
    {
      "id": 1,
      "title": "Spring AI 소개",
      "content": "...",
      "similarity": 0.92,
      "metadata": {...}
    }
  ],
  "confidence": 0.92,
  "processingTimeMs": 1234
}
```

#### 2. **GET /api/rag/query** - 간단한 RAG 질의
```bash
curl http://localhost:8080/api/rag/query?question=Spring%20AI란?
```

#### 3. **GET /api/rag/status** - 서비스 상태 확인
```bash
curl http://localhost:8080/api/rag/status
```

---

### 문서 관리 API

#### 1. **POST /api/documents** - 문서 생성 및 인덱싱
```bash
curl -X POST http://localhost:8080/api/documents \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Spring Boot 가이드",
    "content": "Spring Boot는 ...",
    "metadata": "guide",
    "source": "documentation",
    "category": "framework"
  }'
```

#### 2. **GET /api/documents** - 문서 목록 조회 (페이징)
```bash
curl http://localhost:8080/api/documents?page=0&size=10
```

#### 3. **GET /api/documents/{id}** - 특정 문서 조회
```bash
curl http://localhost:8080/api/documents/1
```

#### 4. **PUT /api/documents/{id}** - 문서 수정
```bash
curl -X PUT http://localhost:8080/api/documents/1 \
  -H "Content-Type: application/json" \
  -d '{...}'
```

#### 5. **DELETE /api/documents/{id}** - 문서 삭제
```bash
curl -X DELETE http://localhost:8080/api/documents/1
```

#### 6. **GET /api/documents/search** - 문서 검색
```bash
curl 'http://localhost:8080/api/documents/search?title=Spring'
```

#### 7. **GET /api/documents/count** - 문서 개수 조회
```bash
curl http://localhost:8080/api/documents/count
```

---

## 🔗 Spring-Vue 서비스와의 연동

### 1. **CORS 설정**
```java
// CorsConfig.java
registry.addMapping("/api/**")
    .allowedOriginPatterns("*")
    .allowedMethods("GET", "POST", "PUT", "DELETE")
    .allowCredentials(true)
    .maxAge(3600);
```

### 2. **API 호출 예제** (Vue.js)
```javascript
// RAG 질의
async function askQuestion(question) {
  const response = await fetch('http://localhost:8080/api/rag/query', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ question, topK: 5 })
  });
  return response.json();
}

// 문서 업로드
async function uploadDocument(title, content) {
  const response = await fetch('http://localhost:8080/api/documents', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ title, content, metadata: 'user-upload' })
  });
  return response.json();
}
```

### 3. **JAR 라이브러리로 사용**
build.gradle에 의존성 추가:
```gradle
dependencies {
    implementation 'com.github.hjyang.rag:wms-rag-service:1.0.0'
}
```

사용:
```java
@Autowired
private RagService ragService;

public void queryRag() {
    RagQueryRequest request = new RagQueryRequest("질문", 5, 0.0);
    RagQueryResponse response = ragService.query(request);
}
```

---

## 🛠️ 기술 스택

| 계층 | 기술 | 역할 |
|------|------|------|
| **프레임워크** | Spring Boot 4.0.6 | 웹 애플리케이션 프레임워크 |
| **AI** | Spring AI 1.0.0-M4 | LLM 통합, 임베딩, 벡터 스토어 |
| **LLM** | OpenAI API | 텍스트 임베딩, 답변 생성 |
| **벡터 스토어** | Chroma | 벡터 임베딩 저장소 |
| **데이터베이스** | H2 | 문서 메타데이터 저장 |
| **ORM** | Spring Data JPA | 데이터베이스 매핑 |
| **빌드 도구** | Gradle | 프로젝트 빌드 및 의존성 관리 |
| **로깅** | Slf4j/Logback | 애플리케이션 로깅 |
| **기타** | Lombok | 보일러플레이트 코드 제거 |

---

## 📝 설정 파일

### application.properties
```properties
# 기본 설정
spring.application.name=wms-rag-service
spring.jpa.hibernate.ddl-auto=update
spring.h2.console.enabled=true

# OpenAI 설정 (환경변수로 설정 권장)
spring.ai.openai.api-key=${OPENAI_API_KEY}
spring.ai.openai.chat.options.model=gpt-4-turbo

# 벡터 스토어 설정
spring.ai.vectorstore.chroma.enabled=true
spring.ai.vectorstore.chroma.url=http://localhost:8000
```

---

## 🚀 실행 방법

### 1. 빌드
```bash
./gradlew build
```

### 2. 실행
```bash
./gradlew bootRun
```

또는 JAR 실행:
```bash
java -jar build/libs/wms-rag-service-0.0.1-SNAPSHOT.jar
```

### 3. 요구사항
- Java 17+
- OpenAI API Key
- Chroma Vector Store (optional, 없으면 메모리 벡터 스토어 사용)

---

## 📊 데이터 흐름

```
사용자 질문
    │
    ▼
RagController.query()
    │
    ├─► VectorStoreService
    │    ├─ 질의 임베딩 (OpenAI)
    │    ├─ 유사도 검색 (Chroma)
    │    └─ 관련 문서 반환
    │
    ├─► 컨텍스트 구성
    │    └─ 검색된 문서 조합
    │
    ├─► 프롬프트 생성
    │    └─ 질문 + 컨텍스트
    │
    ├─► ChatClient
    │    └─ OpenAI API 호출 (답변 생성)
    │
    ▼
RagQueryResponse 반환
    └─ 답변 + 검색 문서 + 신뢰도
```

---

## 🔐 보안 고려사항

1. **API Key 관리**
   - 환경변수로 설정
   - `.env` 파일 사용 (git ignore)

2. **CORS 설정**
   - 신뢰할 수 있는 도메인만 허용

3. **예외 처리**
   - GlobalExceptionHandler로 민감한 정보 노출 방지

4. **로깅**
   - 민감한 정보 로깅 제외

---

## 📚 참고 문서

- [Spring AI 공식 문서](https://spring.io/projects/spring-ai)
- [OpenAI API 문서](https://platform.openai.com/docs)
- [Chroma 문서](https://www.trychroma.com/)

---

## 👨‍💻 개발자 가이드

### 새로운 기능 추가 흐름

1. **DTO 생성** (`dto/` 패키지)
2. **Service 로직 추가** (`service/` 패키지)
3. **Controller 엔드포인트 추가** (`controller/` 패키지)
4. **테스트 작성** (`test/` 디렉토리)
5. **문서 업데이트**

### 코드 컨벤션

- 클래스: PascalCase
- 메서드/변수: camelCase
- 상수: UPPER_SNAKE_CASE
- 패키지: com.github.hjyang.rag.wms_rag_service.{domain}

---

**프로젝트 유지보수**: 2024년 4월
