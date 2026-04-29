package com.github.hjyang.rag.wms_rag_service.controller;

import com.github.hjyang.rag.wms_rag_service.dto.DocumentUploadRequest;
import com.github.hjyang.rag.wms_rag_service.model.Document;
import com.github.hjyang.rag.wms_rag_service.service.DocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 문서 관리 API 컨트롤러
 * 문서의 CRUD 및 검색 기능 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    /**
     * 문서 생성
     */
    @PostMapping
    public ResponseEntity<Document> createDocument(@RequestBody DocumentUploadRequest request) {
        log.info("Creating document: {}", request.getTitle());
        Document document = documentService.createDocument(request);
        return new ResponseEntity<>(document, HttpStatus.CREATED);
    }

    /**
     * 문서 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<Document> getDocument(@PathVariable Long id) {
        Document document = documentService.getDocument(id);
        return ResponseEntity.ok(document);
    }

    /**
     * 모든 문서 조회
     */
    @GetMapping
    public ResponseEntity<Page<Document>> getAllDocuments(Pageable pageable) {
        Page<Document> documents = documentService.getAllDocuments(pageable);
        return ResponseEntity.ok(documents);
    }

    /**
     * 문서 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<Document> updateDocument(
            @PathVariable Long id,
            @RequestBody DocumentUploadRequest request) {
        log.info("Updating document: {}", id);
        Document document = documentService.updateDocument(id, request);
        return ResponseEntity.ok(document);
    }

    /**
     * 문서 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        log.info("Deleting document: {}", id);
        documentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 제목으로 검색
     */
    @GetMapping("/search")
    public ResponseEntity<Page<Document>> searchByTitle(
            @RequestParam String title,
            Pageable pageable) {
        Page<Document> documents = documentService.searchByTitle(title, pageable);
        return ResponseEntity.ok(documents);
    }

    /**
     * 문서 개수 조회
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getDocumentCount() {
        long count = documentService.getDocumentCount();
        return ResponseEntity.ok(count);
    }
}
