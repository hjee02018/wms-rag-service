package com.github.hjyang.rag.wms_rag_service.service;

import com.github.hjyang.rag.wms_rag_service.dto.RagQueryRequest;
import com.github.hjyang.rag.wms_rag_service.dto.RagQueryResponse;
import com.github.hjyang.rag.wms_rag_service.model.Document;
import com.github.hjyang.rag.wms_rag_service.repository.DocumentRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RagService {

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private DocumentRepository documentRepository;

    public void indexDocument(Document document) {
        org.springframework.ai.document.Document aiDoc = new org.springframework.ai.document.Document(document.getContent(),
            Map.of("title", document.getTitle(), "metadata", document.getMetadata()));

        vectorStore.add(List.of(aiDoc));
        documentRepository.save(document);
    }

    public RagQueryResponse query(RagQueryRequest request) {
        List<org.springframework.ai.document.Document> similarDocuments = vectorStore.similaritySearch(
            SearchRequest.query(request.getQuestion()).withTopK(request.getTopK() != null ? request.getTopK() : 5));

        String context = similarDocuments.stream()
            .map(org.springframework.ai.document.Document::getContent)
            .collect(Collectors.joining("\n"));

        String promptTemplate = """
            Based on the following context, answer the question:

            Context:
            {context}

            Question:
            {question}

            Answer:
            """;

        PromptTemplate template = new PromptTemplate(promptTemplate);
        @SuppressWarnings("null")
        Prompt prompt = template.create(Map.of("context", context, "question", request.getQuestion()));

        String answer = chatClient.prompt(prompt).call().content();

        List<RagQueryResponse.RetrievedDocument> retrievedDocs = similarDocuments.stream()
            .map(doc -> RagQueryResponse.RetrievedDocument.builder()
                .title((String) doc.getMetadata().get("title"))
                .content(doc.getContent())
                .metadata(doc.getMetadata().entrySet().stream()
                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().toString())))
                .build())
            .collect(Collectors.toList());

        return RagQueryResponse.builder()
            .answer(answer)
            .retrievedDocuments(retrievedDocs)
            .build();
    }
}