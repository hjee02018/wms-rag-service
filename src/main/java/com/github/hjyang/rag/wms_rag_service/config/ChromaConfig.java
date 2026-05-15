package com.github.hjyang.rag.wms_rag_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.chroma.vectorstore.ChromaVectorStore;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
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
        factory.setReadTimeout(120000);

        RestClient.Builder builder = RestClient.builder()
                .requestFactory(factory);

        return new ChromaApi("http://127.0.0.1:8000", builder, objectMapper);
    }

    @Bean
    public ChromaVectorStore vectorStore(ChromaApi chromaApi,
                                         OllamaEmbeddingModel embeddingModel) { // ← EmbeddingModel → OllamaEmbeddingModel 명시
        return ChromaVectorStore.builder(chromaApi, embeddingModel)
                .collectionName("wms-rag")
                .initializeSchema(true)
                .build();
    }
}