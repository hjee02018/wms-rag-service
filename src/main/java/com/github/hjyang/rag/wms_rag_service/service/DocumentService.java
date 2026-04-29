package com.github.hjyang.rag.wms_rag_service.service;

import com.github.hjyang.rag.wms_rag_service.dto.DocumentUploadRequest;
import com.github.hjyang.rag.wms_rag_service.exception.RagException;
import com.github.hjyang.rag.wms_rag_service.model.Document;
import com.github.hjyang.rag.wms_rag_service.repository.DocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * 문서 관리 서비스
 * 문서의 CRUD 작업 담당
 */
@Slf4j
@Service
public class DocumentService {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private VectorStoreService vectorStoreService;

    /**
     * 문서 생성 및 인덱싱
     */
    public Document createDocument(DocumentUploadRequest request) {
        log.info("Creating document: {}", request.getTitle());
        
        Document document = new Document();
        document.setTitle(request.getTitle());
        document.setContent(request.getContent());
        document.setMetadata(request.getMetadata());
        
        Document savedDocument = documentRepository.save(document);
        
        // 벡터 스토어에 인덱싱
        vectorStoreService.indexDocument(savedDocument);
        
        log.info("Document created successfully: {}", savedDocument.getId());
        return savedDocument;
    }

    /**
     * 문서 조회
     */
    public Document getDocument(Long id) {
        return documentRepository.findById(id)
            .orElseThrow(() -> new RagException("Document not found: " + id));
    }

    /**
     * 모든 문서 조회 (페이징)
     */
    public Page<Document> getAllDocuments(Pageable pageable) {
        return documentRepository.findAll(pageable);
    }

    /**
     * 문서 업데이트
     */
    public Document updateDocument(Long id, DocumentUploadRequest request) {
        log.info("Updating document: {}", id);
        
        Document document = getDocument(id);
        document.setTitle(request.getTitle());
        document.setContent(request.getContent());
        document.setMetadata(request.getMetadata());
        
        Document updatedDocument = documentRepository.save(document);
        
        // 벡터 스토어 재인덱싱
        vectorStoreService.indexDocument(updatedDocument);
        
        log.info("Document updated successfully: {}", id);
        return updatedDocument;
    }

    /**
     * 문서 삭제
     */
    public void deleteDocument(Long id) {
        log.info("Deleting document: {}", id);
        
        Document document = getDocument(id);
        documentRepository.deleteById(id);
        vectorStoreService.deleteDocument(String.valueOf(id));
        
        log.info("Document deleted successfully: {}", id);
    }

    /**
     * 제목으로 문서 검색
     */
    public Page<Document> searchByTitle(String title, Pageable pageable) {
        return documentRepository.findByTitleContaining(title, pageable);
    }

    /**
     * 전체 문서 개수
     */
    public long getDocumentCount() {
        return documentRepository.count();
    }
}
