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
                        질문을 분석해서 답변 생성에 필요한 데이터 소스를 판단하세요.
                        
                        DB: 현재 데이터 조회 (건수, 현황, 수량, 날짜별, 작업, 재고, 에러, 알람, 위치)
                        DOC: 사용법, 절차, 설명, 매뉴얼, 설계서 내용, 운영 방안
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
            dbContext = fetchDbContext(question, chatClient);
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
    private String generateSql(String question, String schema, ChatClient chatClient) {
        // Few-shot 예시 정의
        String fewShotExamples = """
            [SQL 예시]
            
            Q: 입고 작업 몇 건이야?
            A: SELECT COUNT(*) AS 입고건수 FROM HMX_KCTC.V_T_WORK_MONIT WHERE TYPE = '입고'
            
            Q: 출고 작업 몇 건이야?
            A: SELECT COUNT(*) AS 출고건수 FROM HMX_KCTC.V_T_WORK_MONIT WHERE TYPE = '출고'
            
            Q: 현재 작업 현황
            A: SELECT TYPE, STATUS, COUNT(*) AS 건수 FROM HMX_KCTC.V_T_WORK_MONIT GROUP BY TYPE, STATUS ORDER BY TYPE, STATUS
            
            Q: 긴급 작업 목록
            A: SELECT WORK_NO, TYPE, STATUS, ITEM_NM, DEPARTURE, DESTINATION FROM HMX_KCTC.V_T_WORK_MONIT WHERE URGENT = '긴급' FETCH FIRST 20 ROWS ONLY
            
            Q: 진행중인 작업
            A: SELECT WORK_NO, TYPE, ITEM_NM, DEPARTURE, DESTINATION, CURRENTPOINT FROM HMX_KCTC.V_T_WORK_MONIT WHERE STATUS = '진행중' FETCH FIRST 20 ROWS ONLY
            
            Q: 오늘 완료된 작업
            A: SELECT WORK_NO, TYPE, ITEM_NM, DEPARTURE, DESTINATION FROM HMX_KCTC.V_T_WORK_MONIT WHERE STATUS = '완료' AND REG_DATE_TIME LIKE TO_CHAR(SYSDATE,'YYYY-MM-DD') || '%' FETCH FIRST 20 ROWS ONLY
            
            Q: 현재위치 출발지 목적지 포함 작업 현황
            A: SELECT WORK_NO, TYPE, STATUS, ITEM_NM, DEPARTURE, DESTINATION, CURRENTPOINT FROM HMX_KCTC.V_T_WORK_MONIT FETCH FIRST 20 ROWS ONLY
            
            Q: 1호기 작업 건수
            A: SELECT COUNT(*) AS 건수 FROM HMX_KCTC.T_WORK w JOIN HMX_KCTC.T_LOCATION l ON w.DEPARTURE = l.ID_T_LOCATION WHERE l.DEV_NO = 1
            
            Q: 재고 현황
            A: SELECT STATUS, COUNT(*) AS 건수 FROM HMX_KCTC.T_SKU GROUP BY STATUS ORDER BY STATUS
            
            Q: 최근 에러 10건
            A: SELECT RECORD, REG_DATE, REG_TIME FROM HMX_KCTC.T_ERROR ORDER BY REG_DATE DESC, REG_TIME DESC FETCH FIRST 10 ROWS ONLY
            """;

        String sql = chatClient.prompt()
                .system("""
                        Oracle DB 전문가입니다.
                        [스키마 정보]와 [SQL 예시]를 참고하여 질문에 맞는 SQL을 생성하세요.
                        
                        규칙:
                        - SELECT 문만 생성
                        - SQL 코드만 출력 (마크다운 없이)
                        - 반드시 HMX_KCTC.테이블명 형식
                        - FETCH FIRST 20 ROWS ONLY 로 결과 제한
                        - 세미콜론 없이 출력
                        - 조회 불필요하면 NONE 출력
                        
                        도메인 용어:
                        - 호기/stc = T_LOCATION.DEV_NO (1=1호기)
                        - 긴급 = URGENT='1'
                        - 입고 = TYPE='1', 출고 = TYPE='2', 이동 = TYPE='3'
                        - 완료 = STATUS='24', 대기 = STATUS='10'
                        """)
                .user("""
                        [스키마 정보]
                        %s
                        
                        %s
                        
                        [질문]
                        %s
                        
                        SQL:
                        """.formatted(schema, fewShotExamples, question))
                .call()
                .content()
                .replaceAll("```sql", "")
                .replaceAll("```", "")
                .trim();

        return sql;
    }
    // fetchDbContext - schemaContext 파라미터 제거
    private String fetchDbContext(String question, ChatClient chatClient) {
        try {
            // 1. LLM이 필요한 테이블 판단
            String tableNames = identifyTables(question, chatClient);
            log.info("필요한 테이블: {}", tableNames);

            if (tableNames.isBlank() || tableNames.equalsIgnoreCase("NONE")) {
                return "";
            }

            // 2. DB에서 실시간 스키마 조회
            String schema = fetchLiveSchema(tableNames);
            log.info("실시간 스키마: {}자", schema.length());

            // 3. SQL 생성
            String sql = generateSql(question, schema, chatClient);
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

    // LLM이 필요한 테이블명 판단
    private String identifyTables(String question, ChatClient chatClient) {
        String tableList = getTableList();
        return chatClient.prompt()
                .system("""
                        아래 [테이블/뷰 목록]에서 질문에 필요한 것만 골라서
                        이름만 쉼표로 구분해서 출력하세요.
                        조회 불필요하면 NONE 출력.
                        예: V_T_WORK_MONIT
                        예: T_ERROR,T_LOCATION
                        """)
                .user("[테이블 목록]\n" + tableList + "\n\n[질문]\n" + question)
                .call()
                .content()
                .trim()
                .toUpperCase();
    }

    // Oracle에서 테이블 목록 조회
    // 26.05.21 VIEW 도 포함해야함!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    private String getTableList() {
        try {
            // 테이블 + 뷰 둘 다 조회
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT TABLE_NAME AS NAME, 'TABLE' AS OBJ_TYPE 
                FROM ALL_TABLES WHERE OWNER='HMX_KCTC'
                UNION ALL
                SELECT VIEW_NAME AS NAME, 'VIEW' AS OBJ_TYPE
                FROM ALL_VIEWS WHERE OWNER='HMX_KCTC'
                ORDER BY OBJ_TYPE, NAME
                """);

            return rows.stream()
                    .map(r -> r.get("OBJ_TYPE") + ": " + r.get("NAME"))
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            log.error("테이블 목록 조회 오류: {}", e.getMessage());
            return "";
        }
    }

    // Oracle에서 실시간 스키마 (뷰 포함!!) 조회
    private String fetchLiveSchema(String tableNames) {
        StringBuilder schema = new StringBuilder();
        for (String tableName : tableNames.split(",")) {
            tableName = tableName.trim();
            try {
                // VIEW인지 TABLE인지 확인
                List<Map<String, Object>> viewCheck = jdbcTemplate.queryForList(
                    "SELECT VIEW_NAME FROM ALL_VIEWS WHERE OWNER='HMX_KCTC' AND VIEW_NAME=?",
                    tableName);

                if (!viewCheck.isEmpty()) {
                    // VIEW면 Chroma에서 DDL 가져오기
                    String viewDdl = fetchVectorContext("VIEW " + tableName + " DDL 컬럼");
                    if (!viewDdl.isEmpty()) {
                        schema.append("[뷰 DDL]\n").append(viewDdl).append("\n\n");
                    }
                    continue;
                }

                // TABLE이면 기존 방식
                List<Map<String, Object>> cols = jdbcTemplate.queryForList("""
                        SELECT c.COLUMN_NAME, c.DATA_TYPE, c.NULLABLE, cc.COMMENTS
                        FROM ALL_TAB_COLUMNS c
                        LEFT JOIN ALL_COL_COMMENTS cc
                            ON cc.OWNER = c.OWNER
                            AND cc.TABLE_NAME = c.TABLE_NAME
                            AND cc.COLUMN_NAME = c.COLUMN_NAME
                        WHERE c.OWNER = 'HMX_KCTC'
                        AND c.TABLE_NAME = ?
                        ORDER BY c.COLUMN_ID
                        """, tableName);

                if (!cols.isEmpty()) {
                    schema.append("테이블: HMX_KCTC.").append(tableName).append("\n");
                    cols.forEach(col -> schema.append(String.format(
                        "  %s %s%s%s\n",
                        col.get("COLUMN_NAME"),
                        col.get("DATA_TYPE"),
                        "N".equals(col.get("NULLABLE")) ? " NOT NULL" : "",
                        col.get("COMMENTS") != null ? " -- " + col.get("COMMENTS") : ""
                    )));
                    schema.append("\n");
                }
            } catch (Exception e) {
                log.error("스키마 조회 오류 [{}]: {}", tableName, e.getMessage());
            }
        }
        return schema.toString();
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
                            당신은 자동화창고(WCS) 운영 전문가 어시스턴트입니다.
                            - 스태커크레인, RTV, 컨베이어 등 자동화 설비를 잘 압니다.
                            - WMS/WCS 시스템과 물류 프로세스를 깊이 이해합니다.
                            - 현장 운영자와 관리자가 주요 사용자입니다.

                            답변 우선순위:
                                1. [DB 데이터] 있으면 → DB 데이터 기반 답변
                                2. [문서 데이터] 있으면 → 문서 기반 답변  
                                3. 1.2 둘 다 있으면 둘을 종합하여 답변
                                4. 둘 다 없으면 → 보유한 지식으로 자유롭게 답변

                            불확실한 경우:
                                - DB 데이터에 없는 수치는 절대 추측하지 마세요
                                - 정보 부족 시 "해당 데이터를 조회할 수 없습니다" 명확히 답변
                                - 추가 질문이 필요하면 한 가지만 되묻기
                            답변 형식:
                                - DB 조회 결과는 표(테이블) 형식으로 정리
                                - 수치는 단위와 함께 표시 (예: 1,234건)
                                - 복잡한 내용은 번호 목록으로 정리
                                - 핵심 내용을 먼저, 부연 설명은 뒤에
                                - 답변은 한국어로 작성
                            도메인 지식:
                                - 호기(DEV_NO) : 스태커크레인 번호 (1호기, 2호기...)
                                - 작업 유형 (입고/출고/이동): T_WORK.TYPE (1/2/3)
                                - 긴급작업: T_WORK.URGENT = '1'
                                - 완료작업: T_WORK.STATUS = '24'
                                - BCR: 바코드 리더기
                                - RTV: 무인운반차량
                            응답 길이:
                                - 단순 수치 질문: 1~2줄로 간결하게
                                - 현황 요약: 표 형식으로 정리
                                - 절차/방법 질문: 번호 목록으로 단계별 설명
                                - 분석 질문: 요약 → 상세 순으로 답변

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