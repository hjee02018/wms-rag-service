package com.github.hjyang.rag.wms_rag_service.service;

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

    public String query(String question) {
        List<org.springframework.ai.document.Document> similarDocuments = vectorStore.similaritySearch(
            SearchRequest.query(question).withTopK(5));

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
        Prompt prompt = template.create(Map.of("context", context, "question", question));

        return chatClient.prompt(prompt).call().content();
    }
}