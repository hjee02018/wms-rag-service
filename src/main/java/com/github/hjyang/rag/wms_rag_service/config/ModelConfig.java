package com.github.hjyang.rag.wms_rag_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
public class ModelConfig {
    private String currentModel = "qwen3:8b";
}