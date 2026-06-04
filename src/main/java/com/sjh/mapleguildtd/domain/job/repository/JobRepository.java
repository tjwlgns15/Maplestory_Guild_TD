package com.sjh.mapleguildtd.domain.job.repository;

import com.sjh.mapleguildtd.domain.job.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {

    Optional<Job> findByName(String name);

    // 직업군 ID로 직업 목록 조회
    List<Job> findByJobClassId(Long jobClassId);

    // 직업명 부분 일치 검색 (게임 연동 시 활용)
    @Query("SELECT j FROM Job j WHERE j.name LIKE %:keyword%")
    List<Job> searchByName(String keyword);
}

