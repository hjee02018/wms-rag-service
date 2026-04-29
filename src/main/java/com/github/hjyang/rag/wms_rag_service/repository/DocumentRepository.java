package com.github.hjyang.rag.wms_rag_service.repository;

import com.github.hjyang.rag.wms_rag_service.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
}