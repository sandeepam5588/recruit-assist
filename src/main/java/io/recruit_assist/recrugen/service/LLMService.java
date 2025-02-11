package io.recruit_assist.recrugen.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class LLMService {

    private final ChatClient chatClient;

    public LLMService(@Qualifier("ollamaChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String callLLM(String prompt) {
        System.out.println("inside LLMService and prompt is "+ prompt);
        String llmResponse =  chatClient
                .prompt()
                .user(prompt)
                .call()
                .content();

        System.out.println("llm response " + llmResponse);
        return llmResponse;
    }
}

