package com.sjh.mapleguildtd.domain.job.controller;

import com.sjh.mapleguildtd.domain.job.dto.JobConfigResponse;
import com.sjh.mapleguildtd.domain.job.service.JobLookupService;
import com.sjh.mapleguildtd.infrastructure.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobLookupController {

    private final JobLookupService jobLookupService;

    /**
     * 캐릭터 직업명으로 타워 설정 조회
     * 게임 클라이언트에서 길드원 선택 시 호출
     *
     * GET /api/jobs/config?characterClass=히어로
     */
    @GetMapping("/config")
    public ResponseEntity<ApiResponse<JobConfigResponse>> getJobConfig(
            @RequestParam String characterClass) {

        JobConfigResponse config = jobLookupService.lookup(characterClass);
        if (config == null) {
            return ResponseEntity.ok(ApiResponse.error("직업군 정보가 없습니다. 관리자 페이지에서 직업군을 먼저 등록해주세요."));
        }
        return ResponseEntity.ok(ApiResponse.ok(config));
    }
}