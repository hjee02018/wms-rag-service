package com.github.hjyang.rag.wms_rag_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentVectorService {

    private final VectorStore vectorStore;

    public void addDocument(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        String ext = getExtension(filename);

        log.info("문서 업로드 시작: {} ({})", filename, ext);

        List<Document> docs;

        switch (ext) {
            case "pdf" -> docs = readPdf(file);
            case "pptx", "ppt", "docx", "doc", "txt" -> docs = readWithTika(file);
            default -> throw new IllegalArgumentException("지원하지 않는 파일 형식: " + ext);
        }

        if (docs.isEmpty()) {
            log.warn("문서에서 텍스트를 추출하지 못했습니다: {}", filename);
            return;
        }

        // 메타데이터 추가
        docs.forEach(doc -> {
            doc.getMetadata().put("filename", filename);
            doc.getMetadata().put("type", ext);
        });

        // Chunking
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(200)
                .withMinChunkSizeChars(50)
                .withMinChunkLengthToEmbed(5)
                .withKeepSeparator(true)
                .build();

        List<Document> chunks = splitter.apply(docs);
        log.info("청크 수: {}개", chunks.size());

        // Chroma 저장
        vectorStore.add(chunks);
        log.info("벡터 저장 완료: {} → {}청크", filename, chunks.size());
    }

    // PDF 읽기
    private List<Document> readPdf(MultipartFile file) throws IOException {
        ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };

        PagePdfDocumentReader reader = new PagePdfDocumentReader(
                resource,
                PdfDocumentReaderConfig.builder().build().defaultConfig()
        );

        return reader.get();
    }

    // PPTX, DOCX, TXT 등 Tika로 읽기
    private List<Document> readWithTika(MultipartFile file) throws IOException {
        ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };

        TikaDocumentReader reader = new TikaDocumentReader(resource);
        return reader.get();
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}