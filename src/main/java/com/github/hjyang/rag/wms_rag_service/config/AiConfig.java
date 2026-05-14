package com.github.hjyang.rag.wms_rag_service.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    public ChatClient.Builder chatClientBuilder(OllamaChatModel chatModel) {
        return ChatClient.builder(chatModel);
    }
}