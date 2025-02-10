package io.recruit_assist.recrugen.service;

import io.recruit_assist.recrugen.model.JobDescription;
import io.recruit_assist.recrugen.repository.RecruitAssistRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class RecruitAssistService {
    private static final long MAX_SIZE_MB = 3 * 1024 * 1024; // 3MB
    private static final List<String> ALLOWED_TYPES = List.of("application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final RecruitAssistRepository repository;

    public RecruitAssistService(RecruitAssistRepository repository) {
        this.repository = repository;
    }

    public JobDescription uploadJobDescription(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty.");
        }

        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Invalid file type. Only PDF and DOCX are allowed.");
        }

        if (file.getSize() > MAX_SIZE_MB) {
            throw new IllegalArgumentException("File size exceeds 3MB.");
        }

        JobDescription jd = JobDescription.builder()
                .fileName(file.getOriginalFilename())
                .fileType(file.getContentType())
                .fileData(file.getBytes())
                .build();

        return repository.save(jd);
    }
}
