package com.github.hjyang.rag.wms_rag_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 문서 업로드 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadRequest {
    private String title;
    private String content;
    private String metadata;
    private String source;
    private String category;
}
