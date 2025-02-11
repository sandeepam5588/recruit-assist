package io.recruit_assist.recrugen.repository;

import io.recruit_assist.recrugen.model.JD;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JDRepository extends JpaRepository<JD, Long> {
}
