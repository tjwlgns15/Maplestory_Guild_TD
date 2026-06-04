package com.sjh.mapleguildtd.domain.job.service;

import com.sjh.mapleguildtd.domain.job.dto.JobAdminDto;
import com.sjh.mapleguildtd.domain.job.entity.*;
import com.sjh.mapleguildtd.domain.job.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobAdminService {

    private final JobClassRepository jobClassRepository;
    private final SkillRepository    skillRepository;
    private final JobRepository      jobRepository;

    // ── JobClass ──────────────────────────────────────────

    public List<JobAdminDto.JobClassResponse> getAllJobClasses() {
        return jobClassRepository.findAll().stream()
                .map(JobAdminDto.JobClassResponse::from).toList();
    }

    @Transactional
    public JobAdminDto.JobClassResponse createJobClass(JobAdminDto.JobClassRequest req) {
        if (jobClassRepository.findByName(req.getName()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 직업군입니다: " + req.getName());
        }
        return JobAdminDto.JobClassResponse.from(
                jobClassRepository.save(JobClass.builder()
                        .name(req.getName()).range(req.getRange())
                        .cooldown(req.getCooldown()).color(req.getColor())
                        .build())
        );
    }

    @Transactional
    public JobAdminDto.JobClassResponse updateJobClass(Long id, JobAdminDto.JobClassRequest req) {
        JobClass jc = findJobClassById(id);
        jc.update(req.getRange(), req.getCooldown(), req.getColor());
        return JobAdminDto.JobClassResponse.from(jc);
    }

    @Transactional
    public void deleteJobClass(Long id) {
        JobClass jc = findJobClassById(id);
        if (!jc.getJobs().isEmpty()) {
            throw new IllegalStateException("소속 직업이 있는 직업군은 삭제할 수 없습니다.");
        }
        jobClassRepository.delete(jc);
    }

    // ── Skill ─────────────────────────────────────────────

    public List<JobAdminDto.SkillResponse> getAllSkills() {
        return skillRepository.findAll().stream()
                .map(JobAdminDto.SkillResponse::from).toList();
    }

    public List<JobAdminDto.SkillResponse> getSkillsByKind(SkillKind kind) {
        return skillRepository.findBySkillKind(kind).stream()
                .map(JobAdminDto.SkillResponse::from).toList();
    }

    @Transactional
    public JobAdminDto.SkillResponse createSkill(JobAdminDto.SkillRequest req) {
        validateSkillRequest(req);
        return JobAdminDto.SkillResponse.from(
                skillRepository.save(Skill.builder()
                        .name(req.getName()).skillKind(req.getSkillKind())
                        .effectType(req.getEffectType()).description(req.getDescription())
                        .skillColor(req.getSkillColor()).effectValue(req.getEffectValue())
                        .aoeRadius(req.getAoeRadius()).effectDuration(req.getEffectDuration())
                        .cooldown(req.getCooldown()).dotTicks(req.getDotTicks())
                        .dotInterval(req.getDotInterval())
                        .build())
        );
    }

    @Transactional
    public JobAdminDto.SkillResponse updateSkill(Long id, JobAdminDto.SkillRequest req) {
        Skill skill = findSkillById(id);
        validateSkillRequest(req);
        skill.update(req.getName(), req.getEffectType(), req.getDescription(),
                req.getSkillColor(), req.getEffectValue(), req.getAoeRadius(),
                req.getEffectDuration(), req.getCooldown(),
                req.getDotTicks(), req.getDotInterval());
        return JobAdminDto.SkillResponse.from(skill);
    }

    @Transactional
    public void deleteSkill(Long id) {
        skillRepository.delete(findSkillById(id));
    }

    // ── Job ───────────────────────────────────────────────

    public List<JobAdminDto.JobResponse> getAllJobs() {
        return jobRepository.findAll().stream()
                .map(JobAdminDto.JobResponse::from).toList();
    }

    public List<JobAdminDto.JobResponse> getJobsByClass(Long jobClassId) {
        return jobRepository.findByJobClassId(jobClassId).stream()
                .map(JobAdminDto.JobResponse::from).toList();
    }

    @Transactional
    public JobAdminDto.JobResponse createJob(JobAdminDto.JobRequest req) {
        if (jobRepository.findByName(req.getName()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 직업입니다: " + req.getName());
        }
        return JobAdminDto.JobResponse.from(jobRepository.save(buildJob(req)));
    }

    @Transactional
    public JobAdminDto.JobResponse updateJob(Long id, JobAdminDto.JobRequest req) {
        Job job = findJobById(id);
        JobClass jc           = findJobClassById(req.getJobClassId());
        Skill    passiveSkill = resolveSkill(req.getPassiveSkillId(), SkillKind.PASSIVE);
        Skill    activeSkill  = resolveSkill(req.getActiveSkillId(),  SkillKind.ACTIVE);
        job.update(jc, passiveSkill, activeSkill);
        return JobAdminDto.JobResponse.from(job);
    }

    @Transactional
    public void deleteJob(Long id) {
        jobRepository.delete(findJobById(id));
    }

    // ── 내부 헬퍼 ─────────────────────────────────────────

    private Job buildJob(JobAdminDto.JobRequest req) {
        return Job.builder()
                .name(req.getName())
                .jobClass(findJobClassById(req.getJobClassId()))
                .passiveSkill(resolveSkill(req.getPassiveSkillId(), SkillKind.PASSIVE))
                .activeSkill(resolveSkill(req.getActiveSkillId(),  SkillKind.ACTIVE))
                .build();
    }

    private Skill resolveSkill(Long skillId, SkillKind expectedKind) {
        if (skillId == null) return null;
        Skill skill = findSkillById(skillId);
        if (skill.getSkillKind() != expectedKind) {
            throw new IllegalArgumentException(
                    expectedKind.name() + " 스킬이 아닙니다: " + skill.getName());
        }
        return skill;
    }

    private void validateSkillRequest(JobAdminDto.SkillRequest req) {
        if (req.getSkillKind() == SkillKind.ACTIVE && req.getCooldown() == null) {
            throw new IllegalArgumentException("액티브 스킬은 쿨타임을 반드시 입력해야 합니다.");
        }
    }

    private JobClass findJobClassById(Long id) {
        return jobClassRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("직업군을 찾을 수 없습니다: " + id));
    }

    private Skill findSkillById(Long id) {
        return skillRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("스킬을 찾을 수 없습니다: " + id));
    }

    private Job findJobById(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("직업을 찾을 수 없습니다: " + id));
    }
}