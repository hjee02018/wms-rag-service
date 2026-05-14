package com.github.hjyang.rag.wms_rag_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final InventoryQueryService inventoryQueryService;

    public String ask(String question) {

        // 1. DB 직접 조회 (실시간 재고 데이터)
        String dbContext = fetchDbContext(question);

        // 2. Chroma 벡터 검색 (매뉴얼, PDF 등)
        String vectorContext = fetchVectorContext(question);

        log.info("DB context 길이: {}, Vector context 길이: {}",
                dbContext.length(), vectorContext.length());

        // 3. LLM 호출
        return chatClient.prompt()
                .system("""
                        당신은 자동화창고(WCS) 전문가 어시스턴트입니다.
                        아래 두 가지 데이터를 참고하여 답변하세요.
                        - [실시간 재고 데이터]: DB에서 직접 조회한 최신 데이터
                        - [문서 데이터]: 업무 매뉴얼, SOP 등 참고 문서
                        데이터에 없는 내용은 '해당 정보가 없습니다'라고 답하세요.
                        답변은 한국어로 작성하세요.
                        """)
                .user("""
                        [실시간 재고 데이터]
                        %s

                        [문서 데이터]
                        %s

                        [질문]
                        %s
                        """.formatted(dbContext, vectorContext, question))
                .call()
                .content();
    }

    // 질문 키워드 기반으로 적절한 DB 쿼리 선택
    private String fetchDbContext(String question) {
        try {
            if (question.contains("많") || question.contains("상위") || question.contains("최대")) {
                return inventoryQueryService.getTopInventory(10);
            }
            if (question.contains("통계") || question.contains("현황") || question.contains("요약")) {
                return inventoryQueryService.getInventoryStats();
            }
            // 기본: 최근 수정된 재고 현황
            return inventoryQueryService.getInventorySummary();

        } catch (Exception e) {
            log.error("DB 조회 오류: {}", e.getMessage());
            return "DB 조회 중 오류가 발생했습니다.";
        }
    }

    // Chroma에서 관련 문서 검색
    private String fetchVectorContext(String question) {
        try {
            List<Document> docs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(question)
                            .topK(3)
                            .build()
            );

            if (docs.isEmpty()) return "관련 문서가 없습니다.";

            return docs.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n---\n"));

        } catch (Exception e) {
            log.error("벡터 검색 오류: {}", e.getMessage());
            return "문서 검색 중 오류가 발생했습니다.";
        }
    }
}