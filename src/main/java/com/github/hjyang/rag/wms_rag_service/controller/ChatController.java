package com.github.hjyang.rag.wms_rag_service.controller;

import com.github.hjyang.rag.wms_rag_service.config.ModelConfig;
import com.github.hjyang.rag.wms_rag_service.service.DocumentVectorService;
import com.github.hjyang.rag.wms_rag_service.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Map;
import org.springframework.core.env.Environment;
// import java.util.List;
// import java.util.Set;
import java.util.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/rag")
public class ChatController {

    private final RagService ragService;
    private final DocumentVectorService documentVectorService;
    private final ModelConfig modelConfig;
    private final Environment environment;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    private OllamaChatModel ollamaChatModel;

// DB 직접 조회 테스트 API  추가
    @GetMapping("/db-test")
    public ResponseEntity<String> dbTest() {
        try {
            String sql = "SELECT * FROM HMX_KCTC.T_WORK";
            var rows = jdbcTemplate.queryForList(sql);
            StringBuilder sb = new StringBuilder();
            for (var row : rows) {
                sb.append(row.toString()).append("\n");
            }
            return ResponseEntity.ok(sb.toString());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("DB 조회 오류: " + e.getMessage());
        }
    }

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

    // // Ollama 모델 목록 (CORS 우회)
    // @GetMapping("/models")
    // public ResponseEntity<String> getModels() {
    //     try {
    //         RestTemplate rt = new RestTemplate();
    //         String result = rt.getForObject("http://localhost:11434/api/tags", String.class);
    //         return ResponseEntity.ok(result);
    //     } catch (Exception e) {
    //         return ResponseEntity.internalServerError()
    //                 .body("{\"error\":\"" + e.getMessage() + "\"}");
    //     }
    // }
    @GetMapping("/models")
    public ResponseEntity<String> getModels() {
        // 프로파일에 따라 다른 모델 목록 반환
        String profile = environment.getActiveProfiles().length > 0 
            ? environment.getActiveProfiles()[0] : "ollama";

        if ("groq".equals(profile)) {
            // Groq 모델 목록 하드코딩
            String groqModels = """
                {"models":[
                    {"name":"llama-3.1-8b-instant","size":0},
                    {"name":"llama-3.3-70b-versatile","size":0},
                    {"name":"llama-3.3-70b-specdec","size":0},
                    {"name":"llama-3.3-70b-versatile","size":0}
                ]}
                """;
            return ResponseEntity.ok(groqModels);
        }
        if ("gemini".equals(profile)) {
            String geminiModels = """
                {"models":[
                    {"name":"gemini-2.0-flash","size":0},
                    {"name":"gemini-2.0-flash-lite","size":0},
                    {"name":"gemini-1.5-pro","size":0}
                ]}
                """;
            return ResponseEntity.ok(geminiModels);
        }
        // Ollama 모델 목록
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
        boolean dbConfirmed = Boolean.parseBoolean(req.getOrDefault("db_confirmed", "false"));

        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "question is required"));
        }

        String answer = ragService.ask(question, dbConfirmed);

        if (answer.startsWith("__DB_CONFIRM__:")) {
            String dbInfo = answer.replace("__DB_CONFIRM__:", "");
            return ResponseEntity.ok(Map.of(
                "answer", "DB 조회가 필요합니다.",
                "confirm_required", "true",
                "db_info", dbInfo,
                "original_question", question
            ));
        }

        return ResponseEntity.ok(Map.of("answer", answer));
    }
    // 문서 로드
    @GetMapping("/documents")
    public ResponseEntity<List<Map<String, Object>>> getDocuments() {
        try {
            // Chroma에서 메타데이터 조회
            RestTemplate rt = new RestTemplate();
            String url = "http://127.0.0.1:8000/api/v2/tenants/SpringAiTenant/databases/SpringAiDatabase/collections/wms-rag/get";
            
            Map<String, Object> body = Map.of(
                "limit", 100,
                "include", List.of("metadatas")
            );
            
            Map response = rt.postForObject(url, body, Map.class);
            List<Map> metadatas = (List<Map>) response.get("metadatas");
            
            // 파일명 중복 제거
            Set<String> seen = new LinkedHashSet<>();
            List<Map<String, Object>> result = new ArrayList<>();
            
            if (metadatas != null) {
                for (Map meta : metadatas) {
                    String filename = (String) meta.get("filename");
                    String type = (String) meta.get("type");
                    if (filename != null && seen.add(filename)) {
                        result.add(Map.of(
                            "filename", filename,
                            "type", type != null ? type : "unknown"
                        ));
                    }
                }
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("문서 목록 조회 오류: {}", e.getMessage());
            return ResponseEntity.ok(List.of());
        }
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