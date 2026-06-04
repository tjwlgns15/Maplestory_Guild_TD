package com.sjh.mapleguildtd.domain.job.repository;

import com.sjh.mapleguildtd.domain.job.entity.JobClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobClassRepository extends JpaRepository<JobClass, Long> {
    Optional<JobClass> findByName(String name);
}
