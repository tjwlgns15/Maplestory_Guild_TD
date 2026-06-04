package com.sjh.mapleguildtd.domain.job.service;

import com.sjh.mapleguildtd.domain.job.dto.JobConfigResponse;
import com.sjh.mapleguildtd.domain.job.entity.Job;
import com.sjh.mapleguildtd.domain.job.entity.JobClass;
import com.sjh.mapleguildtd.domain.job.repository.JobClassRepository;
import com.sjh.mapleguildtd.domain.job.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobLookupService {

    private final JobRepository      jobRepository;
    private final JobClassRepository jobClassRepository;

    /**
     * 넥슨 API에서 받은 캐릭터 직업명으로 직업 설정 조회
     *
     * 매칭 우선순위:
     * 1. 정확히 일치
     * 2. 부분 포함 (가장 짧은 이름 기준)
     * 3. 직업군 기본값 (직업명으로 직업군 추론)
     */
    public JobConfigResponse lookup(String characterClass) {
        if (characterClass == null || characterClass.isBlank()) {
            return buildDefaultFallback(characterClass);
        }

        // 1. 정확히 일치
        Optional<Job> exact = jobRepository.findByName(characterClass);
        if (exact.isPresent()) return JobConfigResponse.found(exact.get());

        // 2. 부분 포함 (API 반환값과 DB 저장값이 미세하게 다를 수 있음)
        List<Job> partial = jobRepository.searchByName(characterClass);
        if (partial.isEmpty()) {
            // DB 직업명이 API 직업명을 포함하는 경우 (역방향 탐색)
            partial = jobRepository.findAll().stream()
                    .filter(j -> characterClass.contains(j.getName()) || j.getName().contains(characterClass))
                    .toList();
        }
        if (!partial.isEmpty()) {
            // 이름 길이가 가장 가까운 것 선택
            Job best = partial.stream()
                    .min(Comparator.comparingInt(j -> Math.abs(j.getName().length() - characterClass.length())))
                    .get();
            return JobConfigResponse.found(best);
        }

        // 3. 직업군 기본값 폴백
        return buildDefaultFallback(characterClass);
    }

    private JobConfigResponse buildDefaultFallback(String characterClass) {
        String inferredClass = inferJobClass(characterClass);
        Optional<JobClass> jc = jobClassRepository.findByName(inferredClass);
        return jc.map(jobClass -> JobConfigResponse.fallback(characterClass, jobClass))
                .orElse(null);
    }

    /**
     * 직업명 키워드로 직업군 추론
     * DB에 직업이 없을 때 최소한 직업군 스탯이라도 반환하기 위한 폴백
     */
    private String inferJobClass(String job) {
        if (job == null) return "전사";
        if (job.contains("메이지") || job.contains("비숍") || job.contains("에반") ||
                job.contains("루미너스") || job.contains("키네시스") || job.contains("일리움") ||
                job.contains("배틀") || job.contains("라라") || job.contains("칼리")) return "마법사";
        if (job.contains("보우") || job.contains("신궁") || job.contains("패스파") ||
                job.contains("카인") || job.contains("메르세데스") || job.contains("와일드") ||
                job.contains("에인션트")) return "궁수";
        if (job.contains("로드") || job.contains("섀도") || job.contains("듀얼") ||
                job.contains("팬텀") || job.contains("제논") || job.contains("카데나") ||
                job.contains("칸나") || job.contains("호영") || job.contains("엔젤릭")) return "도적";
        if (job.contains("버키") || job.contains("캐논") || job.contains("메카닉") ||
                job.contains("바이퍼") || job.contains("캡틴") || job.contains("천지") ||
                job.contains("진호")) return "해적";
        return "전사"; // 기본값
    }
}