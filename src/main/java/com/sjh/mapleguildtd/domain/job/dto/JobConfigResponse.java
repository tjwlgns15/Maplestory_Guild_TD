package com.sjh.mapleguildtd.domain.job.dto;

import com.sjh.mapleguildtd.domain.job.entity.Job;
import com.sjh.mapleguildtd.domain.job.entity.JobClass;
import com.sjh.mapleguildtd.domain.job.entity.Skill;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 게임에서 타워 생성 시 필요한 직업 설정 응답
 * 직업군(평타 스탯) + 패시브/액티브 스킬 정보를 포함
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class JobConfigResponse {

    private final String  jobName;
    private final String  jobClassName;
    private final int     range;
    private final int     cooldown;
    private final String  color;
    private final boolean found;       // DB에서 직업을 찾았는지 여부
    private final SkillConfig passiveSkill;
    private final SkillConfig activeSkill;

    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class SkillConfig {
        private final Long    id;
        private final String  name;
        private final String  skillKind;
        private final String  effectType;
        private final double  effectValue;
        private final Integer aoeRadius;
        private final Integer effectDuration;
        private final Integer cooldown;
        private final Integer dotTicks;
        private final Integer dotInterval;
        private final String  skillColor;
        private final String  description;

        public static SkillConfig from(Skill s) {
            return new SkillConfig(
                    s.getId(), s.getName(), s.getSkillKind().name(),
                    s.getEffectType().name(), s.getEffectValue(),
                    s.getAoeRadius(), s.getEffectDuration(),
                    s.getCooldown(), s.getDotTicks(), s.getDotInterval(),
                    s.getSkillColor(), s.getDescription()
            );
        }
    }

    /** DB에서 직업을 찾은 경우 */
    public static JobConfigResponse found(Job job) {
        JobClass jc = job.getJobClass();
        return new JobConfigResponse(
                job.getName(), jc.getName(),
                jc.getRange(), jc.getCooldown(), jc.getColor(), true,
                job.getPassiveSkill() != null ? SkillConfig.from(job.getPassiveSkill()) : null,
                job.getActiveSkill()  != null ? SkillConfig.from(job.getActiveSkill())  : null
        );
    }

    /** DB에 직업이 없을 때 직업군 기본값으로 응답 */
    public static JobConfigResponse fallback(String jobName, JobClass jc) {
        return new JobConfigResponse(
                jobName, jc.getName(),
                jc.getRange(), jc.getCooldown(), jc.getColor(), false,
                null, null
        );
    }
}