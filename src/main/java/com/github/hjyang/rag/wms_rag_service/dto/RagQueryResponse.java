package com.github.hjyang.rag.wms_rag_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * RAG 질의 응답 DTO
 * 검색된 문서와 생성된 답변을 포함
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RagQueryResponse {
    private String answer;
    private List<RetrievedDocument> retrievedDocuments;
    private Double confidence;
    private Long processingTimeMs;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RetrievedDocument {
        private Long id;
        private String title;
        private String content;
        private Double similarity;
        private Map<String, String> metadata;
    }
}
