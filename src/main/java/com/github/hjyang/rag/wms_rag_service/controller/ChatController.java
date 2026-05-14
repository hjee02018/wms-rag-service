package com.github.hjyang.rag.wms_rag_service.controller;

import com.github.hjyang.rag.wms_rag_service.config.ModelConfig;
import com.github.hjyang.rag.wms_rag_service.service.DocumentVectorService;
import com.github.hjyang.rag.wms_rag_service.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/rag")
public class ChatController {

    private final RagService ragService;
    private final DocumentVectorService documentVectorService;
    private final ModelConfig modelConfig;

    @Autowired
    private OllamaChatModel ollamaChatModel;

    @PostMapping("/model")
    public ResponseEntity<Map<String, String>> setModel(@RequestBody Map<String, String> req) {
        String model = req.get("model");
        if (model == null || model.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "model is required"));
        }
        modelConfig.setCurrentModel(model);
        log.info("모델 변경: {}", model);
        return ResponseEntity.ok(Map.of("model", model));
    }

    // Ollama 모델 목록 (CORS 우회)
    @GetMapping("/models")
    public ResponseEntity<String> getModels() {
        try {
            RestTemplate rt = new RestTemplate();
            String result = rt.getForObject("http://localhost:11434/api/tags", String.class);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    // 현재 모델 조회
    @GetMapping("/model")
    public ResponseEntity<Map<String, String>> getModel() {
        return ResponseEntity.ok(Map.of("model", modelConfig.getCurrentModel()));
    }

    // RAG 질의응답
    @PostMapping("/ask")
    public ResponseEntity<Map<String, String>> ask(@RequestBody Map<String, String> req) {
        String question = req.get("question");
        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "question is required"));
        }
        return ResponseEntity.ok(Map.of("answer", ragService.ask(question)));
    }

    // 문서 업로드
    @PostMapping("/documents/upload")
    public ResponseEntity<String> uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            documentVectorService.addDocument(file);
            return ResponseEntity.ok("업로드 완료: " + file.getOriginalFilename());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("오류: " + e.getMessage());
        }
    }

    // 헬스체크
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("ok");
    }
}