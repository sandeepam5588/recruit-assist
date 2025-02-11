package io.recruit_assist.recrugen.service;

import io.recruit_assist.recrugen.model.JD;
import io.recruit_assist.recrugen.model.MatchResult;
import io.recruit_assist.recrugen.model.Resume;
import io.recruit_assist.recrugen.repository.JDRepository;
import io.recruit_assist.recrugen.repository.MatchResultRepository;
import io.recruit_assist.recrugen.repository.ResumeRepository;
import org.apache.tika.exception.TikaException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.apache.tika.Tika;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


@Service
public class RecruitAssistService {
    private static final long MAX_FILE_SIZE = 3 * 1024 * 1024; // 3MB
    private static final List<String> ALLOWED_TYPES = List.of("application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final JDRepository jdRepository;
    private final ResumeRepository resumeRepository;
    private final MatchResultRepository matchResultRepository;
    private final LLMService llmService;
    private final Tika tika;

    public RecruitAssistService(JDRepository jdRepository, ResumeRepository resumeRepository, MatchResultRepository matchResultRepository, LLMService llmService) {
        this.jdRepository = jdRepository;
        this.resumeRepository = resumeRepository;
        this.matchResultRepository = matchResultRepository;
        this.llmService = llmService;
        this.tika = new Tika();
    }

    public JD uploadJobDescription(MultipartFile file) throws IOException {
        validateFile(file);
        String extractedData = extractCategories(file);
        JD jd = new JD();
                jd.setFileName(file.getOriginalFilename());
                jd.setFileType(file.getContentType());
                jd.setFileData(file.getBytes());
                jd.setExtractedData(extractedData);
        return jdRepository.save(jd);
    }

    public Resume saveResume(MultipartFile file) throws IOException {
        validateFile(file);

        Resume resume = new Resume();
        resume.setFileName(file.getOriginalFilename());
        resume.setFileType(file.getContentType());
        resume.setFileData(file.getBytes());

        // Call LLM to extract categories
        String extractedData = extractCategories(file);
        resume.setExtractedData(extractedData);

        return resumeRepository.save(resume);
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Invalid file format. Only PDF and DOCX are allowed.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds the 3MB limit.");
        }
    }

    public MatchResult matchJDWithResume(Long jdId, Long resumeId) {
        JD jd = jdRepository.findById(jdId)
                .orElseThrow(() -> new IllegalArgumentException("JD not found"));
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new IllegalArgumentException("Resume not found"));

        if (jd.getExtractedData() == null || resume.getExtractedData() == null) {
            throw new IllegalStateException("Extracted data is missing for JD or Resume");
        }

        // Call LLM to match and score
        String matchData = matchAndScore(jd.getExtractedData(), resume.getExtractedData());

        // Save match result
        MatchResult matchResult = new MatchResult();
        matchResult.setJd(jd);
        matchResult.setResume(resume);
        matchResult.setMatchData(matchData);

        return matchResultRepository.save(matchResult);
    }

    @Async
    public CompletableFuture<MatchResult> matchJDWithResumeAsync(JD jd, Resume resume) {
        return CompletableFuture.supplyAsync(() -> {
            if (jd.getExtractedData() == null || resume.getExtractedData() == null) {
                throw new IllegalStateException("Extracted data is missing for JD or Resume");
            }

            // Call LLM to match and score
            String matchData = matchAndScore(jd.getExtractedData(), resume.getExtractedData());

            // Save match result
            MatchResult matchResult = new MatchResult();
            matchResult.setJd(jd);
            matchResult.setResume(resume);
            matchResult.setMatchData(matchData);

            return matchResultRepository.save(matchResult);
        });
    }

    public CompletableFuture<List<MatchResult>> matchJDWithMultipleResumes(Long jdId, List<Long> resumeIds) {
        JD jd = jdRepository.findById(jdId)
                .orElseThrow(() -> new IllegalArgumentException("JD not found"));

        List<Resume> resumes = resumeRepository.findAllById(resumeIds);
        if (resumes.isEmpty()) {
            throw new IllegalArgumentException("No valid resumes found");
        }

        List<CompletableFuture<MatchResult>> matchFutures = resumes.stream()
                .map(resume -> matchJDWithResumeAsync(jd, resume))
                .toList();

        return CompletableFuture.allOf(matchFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> matchFutures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
    }

    public String extractCategories(MultipartFile file) throws IOException {
        // Extract text from PDF/DOCX file
        String fileContent = null;
        try {
            fileContent = tika.parseToString(file.getInputStream());
        } catch (TikaException e) {
            throw new RuntimeException(e);
        }

        // Construct LLM prompt
        String prompt = """
                Extract the following categories from the given job description or resume:
                
                - Education
                - Responsibilities
                - Technical Skills
                - Soft Skills
                - Good-to-Have Skills
                - Certifications
                - Location
                
                Provide the output in the following JSON format:
                {
                    "education": "...",
                    "responsibilities": "...",
                    "technical_skills": "...",
                    "soft_skills": "...",
                    "good_to_have_skills": "...",
                    "certifications": "...",
                    "location": "..."
                }
                
                File Content:
                %s
                """.formatted(fileContent);

        // Call LLM API
        return llmService.callLLM(prompt);
    }

    public String matchAndScore(String jdData, String resumeData) {
        String prompt = """
                You are an AI assistant skilled in job-resume matching. 
                Given the extracted details of a Job Description (JD) and a Resume, match them and score each category from 1 to 100. 
                Provide comments on what is positive and what is lacking for each category.

                Job Description Data:
                %s
                
                Resume Data:
                %s
                
                Output should be in JSON format with fields:
                {
                    "Role" : "Software Engineer",
                    "Industry" : "Information Technology",
                    "categories": [
                        {
                            "category": "Experience",
                            "weightage" : 25,
                            "score": 85,
                            "comment": "Matches well, but lacks a specific certification."
                        },
                        {
                            "category": "Technical Skills",
                            "weightage" : 20,
                            "score": 70,
                            "comment": "Has most required skills but lacks experience in cloud computing."
                        },
                        {
                            "category": "Education",
                            "weightage" : 10,
                            "score": 85,
                            "comment": "candidate has done Bachelor degree related to the computer science field that matches with the requirement"
                        },
                        {
                            "category": "Responsibilities",
                            "weightage" : 15
                            "score": 85,
                            "comment": "Matches well, but lacks a specific certification."
                        },
                        {
                            "category": "Soft Skills",
                            "weightage" : 10,
                            "score": 70,
                            "comment": "Has most required skills but lacks experience in cloud computing."
                        },
                        {
                            "category": "Good To have skills",
                            "weightage" : 7,
                            "score": 70,
                            "comment": "Has most required skills but lacks experience in cloud computing."
                        },
                        {
                            "category": "Certifications",
                            "weightage" : 8,
                            "score": 70,
                            "comment": "Has most required skills but lacks experience in cloud computing."
                        },
                        {
                            "category": "location",
                            "weightage" : 5,
                            "score": 70,
                            "comment": "Has most required skills but lacks experience in cloud computing."
                        }
                    ],
                    "overall_score": 78,
                    "summary": "Overall, the candidate is a good fit but needs improvement in technical areas."
                }
                """.formatted(jdData, resumeData);

        // Call LLM API
        return llmService.callLLM(prompt);
    }

    public String matchJdAndResumeInstant(MultipartFile jdFile, MultipartFile resumeFile) {
        System.out.println("inside service method");
        validateFile(jdFile);
        validateFile(resumeFile);
        try {
            // Extract text from PDF/DOCX file
            String jdData = null;
            String resumeData = null;
            try {
                jdData = tika.parseToString(jdFile.getInputStream());
                resumeData = tika.parseToString(resumeFile.getInputStream());
            } catch (TikaException e) {
                throw new RuntimeException(e);
            }

            String prompt = """
                You are an AI assistant skilled in job-resume matching. Given the extracted details of a Job Description (JD) and a Resume, match them and score each category from 1 to 100. Provide detailed comments on what is positive and what is lacking for each category. Use specific examples from the Job Description and Resume data to justify the scores and comments.

                Job Description Data:
                %s
                
                Resume Data:
                %s
                
                The output should be in JSON format with the following fields:
                    {
                         "Role": "Software Engineer",
                         "Industry": "Information Technology",
                         "categories": [
                             {
                                 "category": "Experience",
                                 "weightage": 25,
                                 "score": 85,
                                 "comment": "The candidate has 5 years of experience in software development, which aligns well with the JD's requirement of 4+ years. However, the candidate lacks experience in leading large-scale projects, which is a key requirement."
                             },
                             {
                                 "category": "Technical Skills",
                                 "weightage": 20,
                                 "score": 70,
                                 "comment": "The candidate is proficient in Java, Python, and SQL, which are required by the JD. However, the JD also lists cloud computing (AWS) as a requirement, and the candidate has no demonstrated experience in this area."
                             },
                             {
                                 "category": "Education",
                                 "weightage": 10,
                                 "score": 95,
                                 "comment": "The candidate holds a Bachelor's degree in Computer Science, which matches the JD's requirement. Additionally, the candidate has completed relevant coursework in algorithms and data structures, which is a positive."
                             },
                             {
                                 "category": "Responsibilities",
                                 "weightage": 15,
                                 "score": 80,
                                 "comment": "The candidate has experience in developing and maintaining software applications, which aligns with the JD. However, the JD also mentions experience in DevOps practices, which is not reflected in the candidate's resume."
                             },
                             {
                                 "category": "Soft Skills",
                                 "weightage": 10,
                                 "score": 75,
                                 "comment": "The candidate demonstrates strong communication and teamwork skills, as evidenced by their collaboration on cross-functional projects. However, the JD emphasizes leadership skills, which are not prominently highlighted in the resume."
                             },
                             {
                                 "category": "Good To Have Skills",
                                 "weightage": 7,
                                 "score": 60,
                                 "comment": "The JD lists experience with Docker and Kubernetes as good-to-have skills. The candidate has no experience with these tools, which is a gap."
                             },
                             {
                                 "category": "Certifications",
                                 "weightage": 8,
                                 "score": 50,
                                 "comment": "The JD prefers candidates with AWS Certified Solutions Architect certification. The candidate does not have this certification, which is a drawback."
                             },
                             {
                                 "category": "Location",
                                 "weightage": 5,
                                 "score": 100,
                                 "comment": "The candidate is located in the same city as the job location, which is a perfect match."
                             }
                         ],
                         "overall_score": 78,
                         "summary": "The candidate is a strong fit for the role due to their relevant experience, technical skills, and educational background. However, gaps in cloud computing experience, DevOps practices, and certifications need to be addressed to fully meet the JD requirements."
                     }
                """.formatted(jdData, resumeData);

            System.out.println("Before calling the LLM ");
            return llmService.callLLM(prompt);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }




    }


}
