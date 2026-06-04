package com.sjh.mapleguildtd.domain.job.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "skill")
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    // 패시브 / 액티브 구분
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SkillKind skillKind;

    // 효과 종류
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EffectType effectType;

    @Column(length = 200)
    private String description;

    // 투사체/이펙트 색상 (hex)
    @Column(length = 10)
    private String skillColor;

    // ── 수치 ──────────────────────────────────────────────

    // 효과 강도: 데미지 배율(2.0), 증가율(0.2), 등 effectType에 따라 의미가 달라짐
    @Column(nullable = false)
    private double effectValue;

    // 범위 반경 (px, AOE 계열)
    private Integer aoeRadius;

    // 효과 지속 시간 (ms, SLOW/FREEZE/DOT/BUFF 등)
    private Integer effectDuration;

    // ── 액티브 전용 ───────────────────────────────────────

    // 쿨타임 (ms, ACTIVE만 사용)
    private Integer cooldown;

    // DOT 틱 횟수 (DOT 계열)
    private Integer dotTicks;

    // DOT 틱 간격 (ms)
    private Integer dotInterval;

    @Builder
    private Skill(String name, SkillKind skillKind, EffectType effectType,
                  String description, String skillColor, double effectValue,
                  Integer aoeRadius, Integer effectDuration, Integer cooldown,
                  Integer dotTicks, Integer dotInterval) {
        this.name           = name;
        this.skillKind      = skillKind;
        this.effectType     = effectType;
        this.description    = description;
        this.skillColor     = skillColor;
        this.effectValue    = effectValue;
        this.aoeRadius      = aoeRadius;
        this.effectDuration = effectDuration;
        this.cooldown       = cooldown;
        this.dotTicks       = dotTicks;
        this.dotInterval    = dotInterval;
    }

    public void update(String name, EffectType effectType, String description,
                       String skillColor, double effectValue, Integer aoeRadius,
                       Integer effectDuration, Integer cooldown,
                       Integer dotTicks, Integer dotInterval) {
        this.name           = name;
        this.effectType     = effectType;
        this.description    = description;
        this.skillColor     = skillColor;
        this.effectValue    = effectValue;
        this.aoeRadius      = aoeRadius;
        this.effectDuration = effectDuration;
        this.cooldown       = cooldown;
        this.dotTicks       = dotTicks;
        this.dotInterval    = dotInterval;
    }
}