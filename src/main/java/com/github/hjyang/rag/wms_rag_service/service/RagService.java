package com.github.hjyang.rag.wms_rag_service.service;

import com.github.hjyang.rag.wms_rag_service.config.ModelConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final VectorStore vectorStore;
    private final ChatClient.Builder chatClientBuilder;
    private final JdbcTemplate jdbcTemplate;
    private final ModelConfig modelConfig;
    
    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    private String classifyQuestion(String question, ChatClient chatClient) {
        String result = chatClient.prompt()
                .system("""
                        질문을 분석해서 필요한 데이터 소스를 판단하세요.
                        
                        DB: 현재 데이터 조회 (건수, 현황, 수량, 날짜별, 작업, 재고, 에러, 알람, 위치)
                        DOC: 사용법, 절차, 설명, 매뉴얼, 설계서 내용
                        BOTH: DB + 문서 둘 다 필요
                        LLM: 일반 지식으로 답변 가능 (기술 설명, 개념 등)
                        
                        DB, DOC, BOTH, LLM 중 하나만 출력하세요.
                        """)
                .user(question)
                .call()
                .content()
                .trim()
                .toUpperCase();

        if (result.contains("BOTH")) return "BOTH";
        if (result.contains("DB")) return "DB";
        if (result.contains("DOC")) return "DOC";
        return "LLM";
    }
    public String ask(String question, boolean dbConfirmed) {
        ChatClient chatClient = chatClientBuilder.build();
        String type = classifyQuestion(question, chatClient);
        log.info("질문 유형: {}", type);

        // DB 필요한데 확인 안 됐으면 확인 요청
        if ((type.contains("DB") || type.contains("BOTH")) && !dbConfirmed) {
            return "__DB_CONFIRM__:" + extractDbInfo(datasourceUrl);
        }

        String dbContext = "";
        String docContext = "";

        if (type.contains("DB") || type.contains("BOTH")) {
            String schemaContext = fetchVectorContext(question + " 테이블 스키마 컬럼");
            dbContext = fetchDbContext(question, chatClient, schemaContext);
        }
        if (type.contains("DOC") || type.contains("BOTH")) {
            docContext = fetchVectorContext(question);
        }

        log.info("DB: {}자, DOC: {}자", dbContext.length(), docContext.length());
        return generateAnswer(question, dbContext, docContext, chatClient);
    }

    // DB URL에서 정보 추출
    private String extractDbInfo(String url) {
        // jdbc:oracle:thin:@//host:port/service 파싱
        try {
            String cleaned = url.replaceAll("jdbc:oracle:thin:@//", "")
                                .replaceAll("jdbc:oracle:thin:@", "");
            return cleaned.split(";")[0]; // host:port/service 부분만
        } catch (Exception e) {
            return url;
        }
    }

    private String fetchVectorContext(String question) {
        try {
            List<Document> docs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(question)
                            .topK(5)
                            .build()
            );

            log.info("벡터 검색 결과: {}건", docs.size());
            docs.forEach(doc -> log.info("문서 미리보기: {}",
                    doc.getText().substring(0, Math.min(200, doc.getText().length()))));

            if (docs.isEmpty()) return "";
            return docs.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n---\n"));
        } catch (Exception e) {
            log.error("벡터 검색 오류: {}", e.getMessage());
            return "";
        }
    }
    private String extractTableName(String question) {
        String upper = question.toUpperCase();
        String[] tables = {
            "T_WORK", "T_SKU", "T_LOCATION", "T_ERROR",
            "T_HIST", "T_PACKAGE", "T_ARVINF", "T_COMMON",
            "T_USER", "T_AUTHORITY", "T_SEQUENCE", "T_WCS_SET",
            "T_ROLE", "T_MENU", "T_PACLOC", "T_PACITM"
        };
        for (String t : tables) {
            if (upper.contains(t)) return t;
        }
        return "";
    }
    private String generateSql(String question, String schemaContext, ChatClient chatClient) {
        String result = chatClient.prompt()
                .system("""
                        당신은 Oracle DB 전문가입니다.
                        제공된 [스키마 정보]를 참고하여 질문에 맞는 Oracle SQL을 생성하세요.

                        규칙:
                        - SELECT 문만 생성
                        - SQL 코드만 출력 (설명, 마크다운 없이)
                        - 반드시 HMX_KCTC.테이블명 형식으로 작성 (예: HMX_KCTC.T_WORK)
                        - FETCH FIRST 20 ROWS ONLY 로 결과 제한
                        - 스키마명 없이 테이블명만 사용
                        - TOP 문법 사용 금지 (Oracle 미지원)
                        - DB 조회가 필요 없는 질문이면 정확히 'NONE' 만 출력
                        """)
                .user("""
                        [스키마 정보]
                        %s

                        [질문]
                        %s

                        SQL (또는 NONE):
                        """.formatted(schemaContext, question))
                .call()
                .content();

        return result.replaceAll("```sql", "")
                    .replaceAll("```", "")
                    .trim();
    }
    private String fetchDbContext(String question, ChatClient chatClient, String schemaContext) {
        try {
            // 질문에서 테이블명 추출해서 더 정확하게 재검색
            String tableName = extractTableName(question);
            String finalSchema = schemaContext;
            
            if (!tableName.isEmpty()) {
                String tableSchema = fetchVectorContext(tableName + " Table Layout 컬럼");
                if (!tableSchema.isEmpty()) {
                    finalSchema = tableSchema;
                    log.info("테이블 직접 검색: {} ({}자)", tableName, tableSchema.length());
                }
            }

            String sql = generateSql(question, finalSchema, chatClient);
            log.info("생성된 SQL: {}", sql);

            if (sql == null || sql.isBlank() || sql.equalsIgnoreCase("NONE")) {
                return "";
            }
            return executeQuery(sql);
        } catch (Exception e) {
            log.error("DB 조회 오류: {}", e.getMessage());
            return "";
        }
    }

    
    private String executeQuery(String sql) {
        try {
            if (!sql.trim().toUpperCase().startsWith("SELECT")) {
                return "";
            }
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            if (rows.isEmpty()) return "조회 결과 없음";
            return rows.stream()
                    .map(row -> row.entrySet().stream()
                            .map(e -> e.getKey() + "=" + e.getValue())
                            .collect(Collectors.joining(", ")))
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            log.error("SQL 실행 오류: {} \nSQL: {}", e.getMessage(), sql);
            return "";
        }
    }

    private String generateAnswer(String question, String dbContext,
                                   String vectorContext, ChatClient chatClient) {
        StringBuilder context = new StringBuilder();
        if (!dbContext.isEmpty()) {
            context.append("[실시간 DB 데이터]\n").append(dbContext).append("\n\n");
        }
        if (!vectorContext.isEmpty()) {
            context.append("[문서 데이터]\n").append(vectorContext);
        }
        if (context.isEmpty()) {
            context.append("관련 데이터 없음");
        }

        return chatClient.prompt()
                .options(org.springframework.ai.chat.prompt.ChatOptions.builder()
                    .model(modelConfig.getCurrentModel())
                    .build())
                .system("""
                            당신은 자동화창고(WCS) 시스템 및 도메인의 전문가 어시스턴트입니다.
                            답변 우선순위:
                                1. [DB 데이터] 있으면 → DB 데이터 기반 답변
                                2. [문서 데이터] 있으면 → 문서 기반 답변  
                                3. 둘 다 없으면 → 보유한 지식으로 자유롭게 답변
                            
                            DB나 문서 데이터를 추측하거나 만들어내지 마세요.
                            답변은 한국어로 작성하세요.
                        """)
                .user("""
                        %s

                        [질문]
                        %s
                        """.formatted(context.toString(), question))
                .call()
                .content();
    }
}