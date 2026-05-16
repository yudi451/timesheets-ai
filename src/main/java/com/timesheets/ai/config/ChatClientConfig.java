package com.timesheets.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    /**
     * Wraps the auto-configured ChatModel into a ChatClient. The active provider is
     * chosen by spring.ai.model.chat (= 'anthropic' or 'openai'); only that provider's
     * starter contributes a ChatModel bean, so we just inject and wrap it.
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}
