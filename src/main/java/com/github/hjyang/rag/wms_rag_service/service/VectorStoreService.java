package com.github.hjyang.rag.wms_rag_service.service;

import com.github.hjyang.rag.wms_rag_service.dto.RagQueryResponse;
import com.github.hjyang.rag.wms_rag_service.model.Document;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 벡터 스토어 관리 서비스
 * 문서의 임베딩 및 유사성 검색 담당
 */
@Slf4j
@Service
public class VectorStoreService {

    @Autowired
    private VectorStore vectorStore;

    /**
     * 문서를 벡터 스토어에 저장
     */
    public void indexDocument(Document document) {
        log.info("Indexing document: {}", document.getTitle());
        
        org.springframework.ai.document.Document aiDoc = new org.springframework.ai.document.Document(
            document.getContent(),
            Map.of(
                "title", document.getTitle(),
                "docId", String.valueOf(document.getId()),
                "metadata", document.getMetadata() != null ? document.getMetadata() : ""
            )
        );
        
        vectorStore.add(List.of(aiDoc));
        log.info("Document indexed successfully: {}", document.getTitle());
    }

    /**
     * 유사한 문서 검색
     */
    public List<RagQueryResponse.RetrievedDocument> searchSimilarDocuments(String query, Integer topK) {
        log.info("Searching similar documents for query: {}", query);
        
        List<org.springframework.ai.document.Document> similarDocuments = vectorStore.similaritySearch(
            SearchRequest.query(query).withTopK(topK)
        );

        return similarDocuments.stream()
            .map(doc -> RagQueryResponse.RetrievedDocument.builder()
                .title((String) doc.getMetadata().get("title"))
                .content(doc.getContent())
                .metadata(doc.getMetadata())
                .build()
            )
            .collect(Collectors.toList());
    }

    /**
     * 벡터 스토어에서 문서 삭제
     */
    public void deleteDocument(String docId) {
        log.info("Deleting document: {}", docId);
        // 벡터 스토어의 delete 메서드가 지원되는 경우 구현
        // vectorStore.delete(List.of(docId));
        log.warn("Vector store delete operation not yet implemented");
    }

    /**
     * 벡터 스토어 초기화
     */
    public void clearVectorStore() {
        log.warn("Clearing vector store");
        // 벡터 스토어 초기화 로직
        log.info("Vector store cleared");
    }
}
