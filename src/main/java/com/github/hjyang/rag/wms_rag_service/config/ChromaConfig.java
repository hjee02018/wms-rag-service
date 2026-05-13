package com.github.hjyang.rag.wms_rag_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.chroma.vectorstore.ChromaVectorStore;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class ChromaConfig {

    @Bean
    public ChromaApi chromaApi(ObjectMapper objectMapper) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);
        factory.setReadTimeout(60000);

        RestClient.Builder builder = RestClient.builder()
                .requestFactory(factory);

        // IPv4 loopback 주소로 명시: localhost가 IPv6로 해석될 때 연결 실패 방지
        return new ChromaApi("http://127.0.0.1:8000", builder, objectMapper);
    }

    @Bean
    public ChromaVectorStore vectorStore(ChromaApi chromaApi, EmbeddingModel embeddingModel) {
        return ChromaVectorStore.builder(chromaApi, embeddingModel)
                .collectionName("wms-rag")
                .initializeSchema(true)
                .build();
    }
}