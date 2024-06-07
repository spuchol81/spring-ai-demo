package com.tanzu.spring_ai_demo;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
//We only go for openai as we want to use multimodal chat for original posters description
//@ConditionalOnProperty(name = "vectordbsuffix", havingValue = "openai")
public class OpenAiChatConfiguration {

    @Bean
    @Primary
    ChatClient.Builder myChatClientProvider(OpenAiChatModel model) {
         return ChatClient.builder(model);
    }
}
