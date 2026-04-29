package com.github.hjyang.rag.wms_rag_service.controller;

import com.github.hjyang.rag.wms_rag_service.dto.RagQueryRequest;
import com.github.hjyang.rag.wms_rag_service.dto.RagQueryResponse;
import com.github.hjyang.rag.wms_rag_service.service.RagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * RAG 시스템 API 컨트롤러
 * 기존 Spring-Vue 서비스와의 통신을 담당
 */
@Slf4j
@RestController
@RequestMapping("/api/rag")
public class RagController {

    @Autowired
    private RagService ragService;

    /**
     * RAG 질의
     * POST /api/rag/query
     */
    @PostMapping("/query")
    public ResponseEntity<RagQueryResponse> query(@RequestBody RagQueryRequest request) {
        log.info("Received RAG query: {}", request.getQuestion());
        RagQueryResponse response = ragService.query(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 간단한 질의 (호환성 유지)
     * GET /api/rag/query?question=...
     */
    @GetMapping("/query")
    public ResponseEntity<RagQueryResponse> querySimple(@RequestParam String question) {
        log.info("Received simple RAG query: {}", question);
        RagQueryRequest request = new RagQueryRequest(question, 5, 0.0);
        RagQueryResponse response = ragService.query(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 상태 확인
     */
    @GetMapping("/status")
    public ResponseEntity<String> status() {
        return ResponseEntity.ok("RAG service is running");
    }
}