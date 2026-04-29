package com.github.hjyang.rag.wms_rag_service.repository;

import com.github.hjyang.rag.wms_rag_service.model.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 문서 리포지토리
 * 데이터베이스의 문서 정보 조회 및 관리
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    /**
     * 제목으로 문서 검색
     */
    Page<Document> findByTitleContaining(String title, Pageable pageable);

    /**
     * 카테고리로 문서 검색
     */
    List<Document> findByCategory(String category);

    /**
     * 소스로 문서 검색
     */
    List<Document> findBySource(String source);

    /**
     * 제목과 내용으로 검색
     */
    @Query("SELECT d FROM Document d WHERE d.title LIKE %:keyword% OR d.content LIKE %:keyword%")
    Page<Document> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}