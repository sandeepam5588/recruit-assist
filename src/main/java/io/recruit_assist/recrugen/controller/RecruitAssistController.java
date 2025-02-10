package io.recruit_assist.recrugen.controller;

import io.recruit_assist.recrugen.model.JobDescription;
import io.recruit_assist.recrugen.service.RecruitAssistService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
public class RecruitAssistController {

    private final ChatClient chatClient;

    private  RecruitAssistService service;

    public RecruitAssistController(@Qualifier("ollamaChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping("/recrugen/{m}")
    public String chat(@PathVariable String m) {
        return chatClient
                .prompt()
                .user(m)
                .call()
                .content();
    }

    @PostMapping("/upload/jd")
    public ResponseEntity<String> uploadJD(@RequestParam("file") MultipartFile file) {
        try {
            JobDescription jd = service.uploadJobDescription(file);
            return ResponseEntity.ok("File uploaded successfully: " + jd.getFileName());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Error processing file.");
        }
    }
}