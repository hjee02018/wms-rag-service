package com.github.hjyang.rag.wms_rag_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
public class ModelConfig {
    // private String currentModel = "llama-3.1-8b-instant";       // 나쁘지않지만 프롬프트 가이드가 필요
    private String currentModel = "llama-3.3-70b-versatile";
    // private String currentModel = "qwen3:8b";       // ollama 로컬
}