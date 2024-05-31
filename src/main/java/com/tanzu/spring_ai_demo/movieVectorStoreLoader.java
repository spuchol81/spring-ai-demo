package com.tanzu.spring_ai_demo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.JsonReader;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.List;


@Profile("!init")
@Configuration
@ConditionalOnProperty(name = "vectordbsuffix", havingValue = "ollama")
class ollamaLoadConfig {
    @Bean
    public SimpleVectorStore movieVectorStoreLoadBean(@Qualifier("ollamaEmbeddingModel") EmbeddingModel embeddingModel) {
        return new SimpleVectorStore(embeddingModel);
    } 
  }

@Profile("!init")
@Configuration
@ConditionalOnProperty(name = "vectordbsuffix", havingValue = "openai")
class openaiLoadConfig {
   @Bean
   public SimpleVectorStore movieVectorStoreLoadBean(@Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel) {
       return new SimpleVectorStore(embeddingModel);
    } 
  }
  

@Component
@Profile("!init")
public class movieVectorStoreLoader {
    private final Logger logger = LoggerFactory.getLogger(movieVectorStoreLoader.class);
    private final SimpleVectorStore movieVectorStore;
    private final String vectordbsuffix;

    @Autowired
    public movieVectorStoreLoader(SimpleVectorStore movieVectorStore,
                                  @Value("${vectordbsuffix}") String vectordbsuffix) {
        this.movieVectorStore = movieVectorStore;
        this.vectordbsuffix = vectordbsuffix;
    
    }

    @PostConstruct
    public void loadVectorsFromFile() throws IOException {
        File vectorFile = new File("vectors-" + vectordbsuffix + ".json");
        // Load vectors from file
        logger.info("Loading existing vectors from" + vectorFile.getAbsolutePath() );
        if (vectorFile.exists()) {
            movieVectorStore.load(vectorFile);
        }
    }
}   

