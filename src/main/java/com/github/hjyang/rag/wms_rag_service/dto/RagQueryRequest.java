package com.github.hjyang.rag.wms_rag_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RAG 질의 요청 DTO
 * 기존 Spring-Vue 서비스에서 전송하는 질의 요청
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RagQueryRequest {
    private String question;
    private Integer topK;
    private Double similarityThreshold;
}
