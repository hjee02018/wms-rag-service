package com.github.hjyang.rag.wms_rag_service.exception;

/**
 * RAG 시스템 관련 예외 클래스
 */
public class RagException extends RuntimeException {
    
    public RagException(String message) {
        super(message);
    }
    
    public RagException(String message, Throwable cause) {
        super(message, cause);
    }
}
