package com.github.hjyang.rag.wms_rag_service.controller;

import com.github.hjyang.rag.wms_rag_service.model.Document;
import com.github.hjyang.rag.wms_rag_service.service.RagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    @Autowired
    private RagService ragService;

    @PostMapping("/index")
    public ResponseEntity<String> indexDocument(@RequestBody Document document) {
        ragService.indexDocument(document);
        return ResponseEntity.ok("Document indexed successfully");
    }

    @GetMapping("/query")
    public ResponseEntity<String> query(@RequestParam String question) {
        String answer = ragService.query(question);
        return ResponseEntity.ok(answer);
    }
}