package io.recruit_assist.recrugen.controller;

import io.recruit_assist.recrugen.model.JD;
import io.recruit_assist.recrugen.model.MatchResult;
import io.recruit_assist.recrugen.model.Resume;
import io.recruit_assist.recrugen.service.RecruitAssistService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
public class RecruitAssistController {

    private final ChatClient chatClient;

    @Autowired
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
            JD jd = service.uploadJobDescription(file);
            return ResponseEntity.ok("File uploaded successfully: " + jd.getFileName());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Error processing file.");
        }
    }

    @PostMapping("/upload/resume")
    public ResponseEntity<String> uploadResume(@RequestParam("file") MultipartFile file) {
        Resume savedResume = null;
        try {
            savedResume = service.saveResume(file);
            return ResponseEntity.ok("File uploaded successfully: " + savedResume.getFileName());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Error processing file.");
        }
    }

    @PostMapping
    public ResponseEntity<MatchResult> matchJDAndResume(@RequestParam Long jdId, @RequestParam Long resumeId) {
        MatchResult matchResult = service.matchJDWithResume(jdId, resumeId);
        return ResponseEntity.ok(matchResult);
    }

    @PostMapping("/bulk")
    public ResponseEntity<CompletableFuture<List<MatchResult>>> matchJDAndMultipleResumes(
            @RequestParam Long jdId, @RequestBody List<Long> resumeIds) {
        CompletableFuture<List<MatchResult>> results = service.matchJDWithMultipleResumes(jdId, resumeIds);
        return ResponseEntity.ok(results);
    }

    @PostMapping("/upload")
    public ResponseEntity<?> matchJdAndResume(@RequestParam("jd") MultipartFile jdFile, @RequestParam("resume") MultipartFile resumeFile) {
        try {
            System.out.println("received request to /upload");
            // Process the matching logic
            String response = service.matchJdAndResumeInstant(jdFile, resumeFile);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            System.out.println("IllegalArgumentException" + e);
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            System.out.println("Exception" + e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing request");
        }
    }
}