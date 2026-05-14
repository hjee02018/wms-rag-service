package com.github.hjyang.rag.wms_rag_service.loader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DataInitializer implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        log.info("=== RAG 서비스 시작 완료 ===");
        log.info("PDF 업로드: POST /rag/documents/upload");
        log.info("질의응답:   POST /rag/ask");
        log.info("헬스체크:   GET  /rag/health");
    }
}