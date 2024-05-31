package com.tanzu.spring_ai_demo;

import jakarta.annotation.PostConstruct;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.ai.reader.JsonReader;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.tanzu.spring_ai_demo.movieController.ApiController;

import org.springframework.core.env.Environment;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Profile("init")
@Configuration
@ConditionalOnProperty(name = "vectordbsuffix", havingValue = "ollama")
class ollamaInitConfig {
    @Bean
    public SimpleVectorStore movieVectorStoreInitBean(@Qualifier("ollamaEmbeddingModel") EmbeddingModel embeddingModel) {
        return new SimpleVectorStore(embeddingModel);
    } 
  }

@Profile("init")
@Configuration
@ConditionalOnProperty(name = "vectordbsuffix", havingValue = "openai")
class openaiInitConfig {
   @Bean
   public SimpleVectorStore movieVectorStoreInitBean(@Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel) {
       return new SimpleVectorStore(embeddingModel);
    } 
  }
@Component
@Profile("init")
public class movieVectorStoreInitializer {
    private final Logger logger = LoggerFactory.getLogger(movieVectorStoreInitializer.class);
    private final SimpleVectorStore movieVectorStore;
    private final String vectordbsuffix;
    private final Resource resource;

    @Autowired
    public movieVectorStoreInitializer(SimpleVectorStore movieVectorStore,
                                       @Value("${vectordbsuffix}") String vectordbsuffix,
                                       @Value("classpath:static/imdb.json") Resource resource) {
        this.movieVectorStore = movieVectorStore;
        this.vectordbsuffix = vectordbsuffix;
        this.resource = resource;
    }

    @PostConstruct
    public void initializeVectors() throws IOException {
        File vectorFile = new File("vectors-" + vectordbsuffix + ".json");
        // Load Scraped data to vectorstore
        logger.info("Building embeddings and pushing data into " + vectorFile.getAbsolutePath() );
        DocumentReader reader = new JsonReader(resource);
        List<Document> documents = reader.get();
        movieVectorStore.add(documents);
        movieVectorStore.save(vectorFile);
    }
}