package io.recruit_assist.recrugen.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Data
public class MatchResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "jd_id", nullable = false)
    private JD jd;

    @ManyToOne
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @Lob
    private String matchData; // Store JSON response from LLM

    public MatchResult() {
    }

    public MatchResult(JD jd, UUID id, Resume resume, String matchData) {
        this.jd = jd;
        this.id = id;
        this.resume = resume;
        this.matchData = matchData;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public JD getJd() {
        return jd;
    }

    public void setJd(JD jd) {
        this.jd = jd;
    }

    public Resume getResume() {
        return resume;
    }

    public void setResume(Resume resume) {
        this.resume = resume;
    }

    public String getMatchData() {
        return matchData;
    }

    public void setMatchData(String matchData) {
        this.matchData = matchData;
    }
}
