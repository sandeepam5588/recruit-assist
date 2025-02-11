package io.recruit_assist.recrugen.repository;

import io.recruit_assist.recrugen.model.Resume;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ResumeRepository extends JpaRepository<Resume, Long> {
}
