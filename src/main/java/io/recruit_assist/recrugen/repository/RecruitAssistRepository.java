package io.recruit_assist.recrugen.repository;

import io.recruit_assist.recrugen.model.JobDescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecruitAssistRepository extends JpaRepository<JobDescription, Long> {
}
