package com.sjh.mapleguildtd.domain.job.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "job",
        indexes = @Index(name = "idx_job_name", columnList = "name"),
        uniqueConstraints = @UniqueConstraint(name = "uq_job_name", columnNames = "name"))
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_class_id", nullable = false)
    private JobClass jobClass;

    // 패시브 스킬 (없을 수 있음)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passive_skill_id")
    private Skill passiveSkill;

    // 액티브 스킬 (없을 수 있음)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "active_skill_id")
    private Skill activeSkill;

    @Builder
    private Job(String name, JobClass jobClass, Skill passiveSkill, Skill activeSkill) {
        this.name         = name;
        this.jobClass     = jobClass;
        this.passiveSkill = passiveSkill;
        this.activeSkill  = activeSkill;
    }

    public void update(JobClass jobClass, Skill passiveSkill, Skill activeSkill) {
        this.jobClass     = jobClass;
        this.passiveSkill = passiveSkill;
        this.activeSkill  = activeSkill;
    }
}