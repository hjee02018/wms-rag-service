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

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final VectorStore vectorStore;
    private final ChatClient.Builder chatClientBuilder;
    private final JdbcTemplate jdbcTemplate;
    private final ModelConfig modelConfig;

    public String ask(String question) {
        // ChatClient는 기본 빌더로 생성
        ChatClient chatClient = chatClientBuilder.build();

        String vectorContext = fetchVectorContext(question);
        String dbContext = fetchDbContext(question, chatClient, vectorContext);

        log.info("모델: {}, DB: {}자, Vector: {}자",
                modelConfig.getCurrentModel(), dbContext.length(), vectorContext.length());

        return generateAnswer(question, dbContext, vectorContext, chatClient);
    }

    private String fetchVectorContext(String question) {
        try {
            List<Document> docs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(question)
                            .topK(5)
                            .build()
            );
            if (docs.isEmpty()) return "";
            return docs.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n---\n"));
        } catch (Exception e) {
            log.error("벡터 검색 오류: {}", e.getMessage());
            return "";
        }
    }

    private String fetchDbContext(String question, ChatClient chatClient, String schemaContext) {
        try {
            String sql = generateSql(question, schemaContext, chatClient);
            log.info("생성된 SQL: {}", sql);
            return executeQuery(sql);
        } catch (Exception e) {
            log.error("DB 조회 오류: {}", e.getMessage());
            return "";
        }
    }

    private String generateSql(String question, String schemaContext, ChatClient chatClient) {
        String sql = chatClient.prompt()
                .options(org.springframework.ai.chat.prompt.ChatOptions.builder()
                    .model(modelConfig.getCurrentModel())
                    .build())
                .system("""
                        당신은 Oracle DB 전문가입니다.
                        아래 [스키마 정보]를 참고하여 Oracle SQL을 생성하세요.

                        규칙:
                        - SELECT 문만 생성
                        - SQL 코드만 출력 (설명, 마크다운 없이)
                        - FETCH FIRST 20 ROWS ONLY 로 결과 제한
                        - 스키마명 없이 테이블명만 사용
                        - TOP 문법 사용 금지
                        """)
                .user("""
                        [스키마 정보]
                        %s

                        [질문]
                        %s

                        SQL:
                        """.formatted(schemaContext, question))
                .call()
                .content();

        return sql.replaceAll("```sql", "")
                  .replaceAll("```", "")
                  .trim();
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
                        당신은 자동화창고(WCS) 전문가 어시스턴트입니다.
                        제공된 데이터를 기반으로 질문에 답변하세요.
                        데이터에 없는 내용은 '해당 정보가 없습니다'라고 답하세요.
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