package com.sjh.mapleguildtd.domain.job.dto;

import com.sjh.mapleguildtd.domain.job.entity.*;
import lombok.Getter;

public class JobAdminDto {

    // ── JobClass (기존과 동일) ─────────────────────────────

    @Getter
    public static class JobClassRequest {
        private String name;
        private int    range;
        private int    cooldown;
        private String color;
    }

    @Getter
    public static class JobClassResponse {
        private final Long   id;
        private final String name;
        private final int    range;
        private final int    cooldown;
        private final String color;

        private JobClassResponse(Long id, String name, int range, int cooldown, String color) {
            this.id = id; this.name = name; this.range = range;
            this.cooldown = cooldown; this.color = color;
        }

        public static JobClassResponse from(JobClass jc) {
            return new JobClassResponse(jc.getId(), jc.getName(), jc.getRange(), jc.getCooldown(), jc.getColor());
        }
    }

    // ── Skill ─────────────────────────────────────────────

    @Getter
    public static class SkillRequest {
        private String     name;
        private SkillKind  skillKind;    // PASSIVE | ACTIVE
        private EffectType effectType;
        private String     description;
        private String     skillColor;
        private double     effectValue;
        private Integer    aoeRadius;
        private Integer    effectDuration;
        private Integer    cooldown;     // ACTIVE 전용
        private Integer    dotTicks;
        private Integer    dotInterval;
    }

    @Getter
    public static class SkillResponse {
        private final Long       id;
        private final String     name;
        private final SkillKind  skillKind;
        private final EffectType effectType;
        private final String     description;
        private final String     skillColor;
        private final double     effectValue;
        private final Integer    aoeRadius;
        private final Integer    effectDuration;
        private final Integer    cooldown;
        private final Integer    dotTicks;
        private final Integer    dotInterval;

        private SkillResponse(Skill s) {
            this.id             = s.getId();
            this.name           = s.getName();
            this.skillKind      = s.getSkillKind();
            this.effectType     = s.getEffectType();
            this.description    = s.getDescription();
            this.skillColor     = s.getSkillColor();
            this.effectValue    = s.getEffectValue();
            this.aoeRadius      = s.getAoeRadius();
            this.effectDuration = s.getEffectDuration();
            this.cooldown       = s.getCooldown();
            this.dotTicks       = s.getDotTicks();
            this.dotInterval    = s.getDotInterval();
        }

        public static SkillResponse from(Skill s) { return new SkillResponse(s); }
    }

    // ── Job ───────────────────────────────────────────────

    @Getter
    public static class JobRequest {
        private String  name;
        private Long    jobClassId;
        private Long    passiveSkillId;  // nullable
        private Long    activeSkillId;   // nullable
    }

    @Getter
    public static class JobResponse {
        private final Long         id;
        private final String       name;
        private final Long         jobClassId;
        private final String       jobClassName;
        private final SkillResponse passiveSkill;  // null 가능
        private final SkillResponse activeSkill;   // null 가능

        private JobResponse(Long id, String name, Long jobClassId, String jobClassName,
                            SkillResponse passiveSkill, SkillResponse activeSkill) {
            this.id           = id;
            this.name         = name;
            this.jobClassId   = jobClassId;
            this.jobClassName = jobClassName;
            this.passiveSkill = passiveSkill;
            this.activeSkill  = activeSkill;
        }

        public static JobResponse from(Job job) {
            return new JobResponse(
                    job.getId(),
                    job.getName(),
                    job.getJobClass().getId(),
                    job.getJobClass().getName(),
                    job.getPassiveSkill() != null ? SkillResponse.from(job.getPassiveSkill()) : null,
                    job.getActiveSkill()  != null ? SkillResponse.from(job.getActiveSkill())  : null
            );
        }
    }
}