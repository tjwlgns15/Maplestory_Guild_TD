package com.sjh.mapleguildtd.domain.job.controller;

import com.sjh.mapleguildtd.domain.job.dto.JobAdminDto;
import com.sjh.mapleguildtd.domain.job.entity.SkillKind;
import com.sjh.mapleguildtd.domain.job.service.JobAdminService;
import com.sjh.mapleguildtd.infrastructure.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class JobAdminController {

    private final JobAdminService jobAdminService;

    // ── JobClass ──────────────────────────────────────────

    @GetMapping("/job-classes")
    public ResponseEntity<ApiResponse<List<JobAdminDto.JobClassResponse>>> getJobClasses() {
        return ResponseEntity.ok(ApiResponse.ok(jobAdminService.getAllJobClasses()));
    }

    @PostMapping("/job-classes")
    public ResponseEntity<ApiResponse<JobAdminDto.JobClassResponse>> createJobClass(
            @RequestBody JobAdminDto.JobClassRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(jobAdminService.createJobClass(req)));
    }

    @PutMapping("/job-classes/{id}")
    public ResponseEntity<ApiResponse<JobAdminDto.JobClassResponse>> updateJobClass(
            @PathVariable Long id, @RequestBody JobAdminDto.JobClassRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(jobAdminService.updateJobClass(id, req)));
    }

    @DeleteMapping("/job-classes/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteJobClass(@PathVariable Long id) {
        jobAdminService.deleteJobClass(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ── Skill ─────────────────────────────────────────────

    @GetMapping("/skills")
    public ResponseEntity<ApiResponse<List<JobAdminDto.SkillResponse>>> getSkills(
            @RequestParam(required = false) SkillKind kind) {
        List<JobAdminDto.SkillResponse> skills = kind != null
                ? jobAdminService.getSkillsByKind(kind)
                : jobAdminService.getAllSkills();
        return ResponseEntity.ok(ApiResponse.ok(skills));
    }

    @PostMapping("/skills")
    public ResponseEntity<ApiResponse<JobAdminDto.SkillResponse>> createSkill(
            @RequestBody JobAdminDto.SkillRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(jobAdminService.createSkill(req)));
    }

    @PutMapping("/skills/{id}")
    public ResponseEntity<ApiResponse<JobAdminDto.SkillResponse>> updateSkill(
            @PathVariable Long id, @RequestBody JobAdminDto.SkillRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(jobAdminService.updateSkill(id, req)));
    }

    @DeleteMapping("/skills/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSkill(@PathVariable Long id) {
        jobAdminService.deleteSkill(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ── Job ───────────────────────────────────────────────

    @GetMapping("/jobs")
    public ResponseEntity<ApiResponse<List<JobAdminDto.JobResponse>>> getJobs(
            @RequestParam(required = false) Long jobClassId) {
        List<JobAdminDto.JobResponse> jobs = jobClassId != null
                ? jobAdminService.getJobsByClass(jobClassId)
                : jobAdminService.getAllJobs();
        return ResponseEntity.ok(ApiResponse.ok(jobs));
    }

    @PostMapping("/jobs")
    public ResponseEntity<ApiResponse<JobAdminDto.JobResponse>> createJob(
            @RequestBody JobAdminDto.JobRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(jobAdminService.createJob(req)));
    }

    @PutMapping("/jobs/{id}")
    public ResponseEntity<ApiResponse<JobAdminDto.JobResponse>> updateJob(
            @PathVariable Long id, @RequestBody JobAdminDto.JobRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(jobAdminService.updateJob(id, req)));
    }

    @DeleteMapping("/jobs/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteJob(@PathVariable Long id) {
        jobAdminService.deleteJob(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}