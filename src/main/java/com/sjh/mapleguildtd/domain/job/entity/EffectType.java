package com.sjh.mapleguildtd.domain.job.entity;

public enum EffectType {

    // ── 패시브 전용 ────────────────────────────────────────
    DAMAGE_BOOST,       // 평타 데미지 N% 증가
    RANGE_BOOST,        // 사거리 N px 증가
    ATTACK_SPEED_BOOST, // 공격속도 N% 증가 (쿨다운 감소)
    ON_HIT_SLOW,        // 평타 명중 시 슬로우 적용
    ON_HIT_DOT,         // 평타 명중 시 지속 피해 적용

    // ── 액티브 전용 ────────────────────────────────────────
    AOE_DAMAGE,         // 범위 내 모든 적에게 데미지
    SINGLE_BURST,       // 단일 대상 강타 (높은 배율)
    SLOW_FIELD,         // 범위 내 슬로우
    FREEZE_FIELD,       // 범위 내 빙결
    DOT_FIELD,          // 범위 내 지속 피해
    BUFF_TOWERS,        // 주변 아군 타워 데미지 증가
}