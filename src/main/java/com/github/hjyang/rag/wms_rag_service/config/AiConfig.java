package com.github.hjyang.rag.wms_rag_service.config;

import org.springframework.ai.chat.model.ChatModel;

/**
 * Spring AI 설정
 * ChatClient 및 VectorStore 설정
 */
@Slf4j
@Configuration
public class AiConfig {

    /**
     * ChatClient Bean 생성
     * Ollama와의 통신을 담당
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        log.info("Initializing ChatClient with ChatModel");
        return ChatClient.builder(chatModel).build();
    }
}