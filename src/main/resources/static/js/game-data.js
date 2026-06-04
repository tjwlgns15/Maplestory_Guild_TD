'use strict';

const API_BASE = 'http://localhost:8088';

// ─────────────────────────────────────────────────────────
// 직업 설정 캐시 (같은 직업 중복 조회 방지)
// ─────────────────────────────────────────────────────────
const _jobConfigCache = {};

/**
 * 서버에서 직업 설정 조회 (패시브/액티브 스킬 포함)
 * guild.html에서 선택된 멤버 정보와 결합하여 타워 설정을 완성
 */
async function fetchJobConfig(characterClass) {
    if (_jobConfigCache[characterClass]) return _jobConfigCache[characterClass];
    try {
        const res  = await fetch(`${API_BASE}/api/jobs/config?characterClass=${encodeURIComponent(characterClass)}`);
        const body = await res.json();
        if (body.success && body.data) {
            _jobConfigCache[characterClass] = body.data;
            return body.data;
        }
    } catch (e) {
        console.warn(`직업 설정 조회 실패: ${characterClass}`, e);
    }
    return null;
}

// ─────────────────────────────────────────────────────────
// 타워 설정 빌더
// ─────────────────────────────────────────────────────────

/**
 * 길드원 데이터 + DB 직업 설정 → 타워 설정 생성
 *
 * @param {Object} member        - { characterName, job, level, combatPower, characterImage }
 * @param {Object} jobConfig     - fetchJobConfig() 응답 (null 가능)
 * @returns {Object}             - 게임에서 사용할 타워 설정 객체
 */
function buildTowerConfig(member, jobConfig) {
    // DB에 직업 정보가 없으면 직업군 기본값 사용
    const range    = jobConfig?.range    ?? 120;
    const cooldown = jobConfig?.cooldown ?? 1500;
    const color    = jobConfig?.color    ?? '#7f8c8d';

    // 전투력 스케일: 100만 CP = 1.0
    const cpScale = Math.max(member.combatPower, 1) / 1_000_000;

    return {
        name:         member.characterName,
        job:          member.job,
        jobClassName: jobConfig?.jobClassName ?? '알 수 없음',
        image:        member.characterImage || null,
        range,
        cooldown,
        color,

        // 기본 평타 데미지 (패시브 보정 전)
        baseDamage:   cpScale * 10,

        // 패시브 스킬 (없으면 null)
        passiveSkill: jobConfig?.passiveSkill ?? null,

        // 액티브 스킬 (없으면 null)
        activeSkill:  jobConfig?.activeSkill  ?? null,

        // DB 매칭 여부 (false면 기본값 사용 중)
        isDefaultConfig: !(jobConfig?.found ?? false),
    };
}

/**
 * 패시브 스킬이 평타에 적용되는 최종 데미지 계산
 *
 * @param {Object} towerConfig  - buildTowerConfig() 결과
 * @returns {number}            - 패시브 보정 후 평타 데미지
 */
function calcAttackDamage(towerConfig) {
    const base    = towerConfig.baseDamage;
    const passive = towerConfig.passiveSkill;
    if (!passive) return base;

    switch (passive.effectType) {
        case 'DAMAGE_BOOST':
            return base * (1 + passive.effectValue);
        case 'ATTACK_SPEED_BOOST':
            // 데미지는 그대로, 쿨다운 감소로 처리 (Tower 클래스에서 사용)
            return base;
        default:
            return base;
    }
}

/**
 * 패시브 스킬이 적용된 최종 쿨다운 계산
 *
 * @param {Object} towerConfig
 * @returns {number} ms
 */
function calcCooldown(towerConfig) {
    const base    = towerConfig.cooldown;
    const passive = towerConfig.passiveSkill;
    if (!passive) return base;

    if (passive.effectType === 'ATTACK_SPEED_BOOST') {
        return Math.max(base * (1 - passive.effectValue), 200); // 최소 200ms
    }
    return base;
}

/**
 * 패시브 스킬이 적용된 최종 사거리 계산
 *
 * @param {Object} towerConfig
 * @returns {number} px
 */
function calcRange(towerConfig) {
    const base    = towerConfig.range;
    const passive = towerConfig.passiveSkill;
    if (!passive) return base;

    if (passive.effectType === 'RANGE_BOOST') {
        return base + passive.effectValue;
    }
    return base;
}