package com.github.hjyang.rag.wms_rag_service.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class AiConfig {

    // Groq 프로파일 — OpenAI 호환 모델 사용
    @Bean
    @Profile("groq")
    public ChatClient.Builder chatClientBuilder(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel);
    }

    // Ollama 프로파일 — 로컬 모델 사용
    @Bean
    @Profile("ollama")
    public ChatClient.Builder chatClientBuilderOllama(OllamaChatModel chatModel) {
        return ChatClient.builder(chatModel);
    }
}