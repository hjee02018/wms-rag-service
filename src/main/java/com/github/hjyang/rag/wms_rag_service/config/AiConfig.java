package com.github.hjyang.rag.wms_rag_service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 설정
 * ChatClient 및 VectorStore 설정
 */
@Slf4j
@Configuration
public class AiConfig {

    /**
     * ChatClient Bean 생성
     * OpenAI API와의 통신을 담당
     */
    @Bean
    public ChatClient chatClient(OpenAiChatModel chatModel) {
        log.info("Initializing ChatClient with OpenAiChatModel");
        return ChatClient.builder(chatModel).build();
    }
}