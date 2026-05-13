package com.github.hjyang.rag.wms_rag_service;

import org.springframework.ai.vectorstore.chroma.autoconfigure.ChromaVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {ChromaVectorStoreAutoConfiguration.class})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}