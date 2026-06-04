package com.sjh.mapleguildtd.infrastructure.init;

import com.sjh.mapleguildtd.domain.job.entity.*;
import com.sjh.mapleguildtd.domain.job.repository.JobClassRepository;
import com.sjh.mapleguildtd.domain.job.repository.JobRepository;
import com.sjh.mapleguildtd.domain.job.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class DataInitService {

    private final JobClassRepository jobClassRepository;
    private final SkillRepository skillRepository;
    private final JobRepository jobRepository;

    @Transactional(readOnly = true)
    public boolean isAlreadyInitialized() {
        return jobClassRepository.count() > 0;
    }

    @Transactional
    public void initialize() {
        Map<String, JobClass> classes = initJobClasses();
        Map<String, Skill>    skills  = initSkills();
        initJobs(classes, skills);
    }

    // ── 직업군 ────────────────────────────────────────────
    private Map<String, JobClass> initJobClasses() {
        JobClass warrior = jobClassRepository.save(JobClass.builder()
                .name("전사").range(90).cooldown(2000).color("#c0392b").build());
        JobClass mage = jobClassRepository.save(JobClass.builder()
                .name("마법사").range(160).cooldown(1200).color("#8e44ad").build());
        JobClass archer = jobClassRepository.save(JobClass.builder()
                .name("궁수").range(220).cooldown(800).color("#27ae60").build());
        JobClass thief = jobClassRepository.save(JobClass.builder()
                .name("도적").range(110).cooldown(500).color("#d35400").build());
        JobClass pirate = jobClassRepository.save(JobClass.builder()
                .name("해적").range(130).cooldown(1000).color("#2980b9").build());

        return Map.of("전사", warrior, "마법사", mage, "궁수", archer, "도적", thief, "해적", pirate);
    }

    // ── 스킬 ──────────────────────────────────────────────
    private Map<String, Skill> initSkills() {

        // ── 패시브 ────────────────────────────────────────
        Skill pWarrior = skillRepository.save(Skill.builder()
                .name("불굴의 의지").skillKind(SkillKind.PASSIVE).effectType(EffectType.DAMAGE_BOOST)
                .effectValue(0.15).skillColor("#e74c3c")
                .description("평타 데미지 15% 증가").build());

        Skill pMage = skillRepository.save(Skill.builder()
                .name("마력 증폭").skillKind(SkillKind.PASSIVE).effectType(EffectType.DAMAGE_BOOST)
                .effectValue(0.20).skillColor("#9b59b6")
                .description("평타 데미지 20% 증가").build());

        Skill pArcher = skillRepository.save(Skill.builder()
                .name("예리한 눈").skillKind(SkillKind.PASSIVE).effectType(EffectType.RANGE_BOOST)
                .effectValue(40).skillColor("#2ecc71")
                .description("사거리 40px 증가").build());

        Skill pThief = skillRepository.save(Skill.builder()
                .name("그림자 발걸음").skillKind(SkillKind.PASSIVE).effectType(EffectType.ATTACK_SPEED_BOOST)
                .effectValue(0.20).skillColor("#f39c12")
                .description("공격속도 20% 증가").build());

        Skill pPirate = skillRepository.save(Skill.builder()
                .name("근성").skillKind(SkillKind.PASSIVE).effectType(EffectType.DAMAGE_BOOST)
                .effectValue(0.10).skillColor("#3498db")
                .description("평타 데미지 10% 증가").build());

        Skill pPoison = skillRepository.save(Skill.builder()
                .name("독 도포").skillKind(SkillKind.PASSIVE).effectType(EffectType.ON_HIT_DOT)
                .effectValue(0.30).skillColor("#2ecc71").effectDuration(1800).dotTicks(3).dotInterval(600)
                .description("평타 명중 시 지속 피해 적용").build());

        Skill pSlow = skillRepository.save(Skill.builder()
                .name("발목 잡기").skillKind(SkillKind.PASSIVE).effectType(EffectType.ON_HIT_SLOW)
                .effectValue(0.50).skillColor("#74b9ff").effectDuration(1500)
                .description("평타 명중 시 이동속도 50% 감소").build());

        // ── 액티브 ────────────────────────────────────────
        Skill aBurst = skillRepository.save(Skill.builder()
                .name("일섬").skillKind(SkillKind.ACTIVE).effectType(EffectType.SINGLE_BURST)
                .effectValue(5.0).cooldown(8000).skillColor("#e74c3c")
                .description("단일 대상에게 500% 강타").build());

        Skill aAoe = skillRepository.save(Skill.builder()
                .name("대폭발").skillKind(SkillKind.ACTIVE).effectType(EffectType.AOE_DAMAGE)
                .effectValue(2.5).aoeRadius(90).cooldown(10000).skillColor("#e67e22")
                .description("반경 90px 범위 광역 피해").build());

        Skill aFreeze = skillRepository.save(Skill.builder()
                .name("빙결장").skillKind(SkillKind.ACTIVE).effectType(EffectType.FREEZE_FIELD)
                .effectValue(1.5).aoeRadius(100).cooldown(12000).effectDuration(2000).skillColor("#74b9ff")
                .description("반경 100px 범위 2초 빙결").build());

        Skill aDotField = skillRepository.save(Skill.builder()
                .name("독무장").skillKind(SkillKind.ACTIVE).effectType(EffectType.DOT_FIELD)
                .effectValue(0.8).aoeRadius(80).cooldown(10000).dotTicks(4).dotInterval(700).skillColor("#2ecc71")
                .description("범위 내 적에게 지속 피해").build());

        Skill aSlowField = skillRepository.save(Skill.builder()
                .name("그물 투척").skillKind(SkillKind.ACTIVE).effectType(EffectType.SLOW_FIELD)
                .effectValue(0.5).aoeRadius(100).cooldown(8000).effectDuration(3000).skillColor("#3498db")
                .description("반경 100px 범위 3초 슬로우").build());

        Skill aBuff = skillRepository.save(Skill.builder()
                .name("결의의 외침").skillKind(SkillKind.ACTIVE).effectType(EffectType.BUFF_TOWERS)
                .effectValue(0.30).aoeRadius(150).cooldown(15000).effectDuration(5000).skillColor("#f1c40f")
                .description("주변 타워 데미지 30% 증가 (5초)").build());

        return Map.ofEntries(
                Map.entry("pWarrior", pWarrior), Map.entry("pMage", pMage),
                Map.entry("pArcher", pArcher),   Map.entry("pThief", pThief),
                Map.entry("pPirate", pPirate),   Map.entry("pPoison", pPoison),
                Map.entry("pSlow", pSlow),        Map.entry("aBurst", aBurst),
                Map.entry("aAoe", aAoe),          Map.entry("aFreeze", aFreeze),
                Map.entry("aDotField", aDotField),Map.entry("aSlowField", aSlowField),
                Map.entry("aBuff", aBuff)
        );
    }

    // ── 직업 ──────────────────────────────────────────────
    private void initJobs(Map<String, JobClass> c, Map<String, Skill> s) {
        // 전사
        saveJob("히어로",       c.get("전사"),  s.get("pWarrior"), s.get("aBurst"));
        saveJob("팔라딘",       c.get("전사"),  s.get("pWarrior"), s.get("aBuff"));
        saveJob("다크나이트",    c.get("전사"),  s.get("pPoison"),  s.get("aBurst"));
        saveJob("미하일",       c.get("전사"),  null,       s.get("aBuff"));
        saveJob("소울마스터",    c.get("전사"),  s.get("pWarrior"), s.get("aAoe"));
        saveJob("아란",         c.get("전사"),  s.get("pWarrior"), s.get("aAoe"));
        saveJob("블래스터",      c.get("전사"),  s.get("pWarrior"), s.get("aBurst"));
        saveJob("데몬슬레이어",   c.get("전사"),  s.get("pWarrior"), s.get("aAoe"));
        saveJob("데몬어벤져",     c.get("전사"),  s.get("pWarrior"), s.get("aAoe"));
        saveJob("카이저",        c.get("전사"),  s.get("pWarrior"), s.get("aBurst"));
        saveJob("아델",          c.get("전사"),  s.get("pWarrior"), s.get("aBurst"));
        saveJob("제로",          c.get("전사"),  s.get("pWarrior"), s.get("aBurst"));
        saveJob("렌",            c.get("전사"),  s.get("pWarrior"), s.get("aBurst"));

        // 마법사
        saveJob("아크메이지(불,독)",    c.get("마법사"), s.get("pPoison"),  s.get("aDotField"));
        saveJob("아크메이지(썬,콜)",    c.get("마법사"), s.get("pMage"),   s.get("aFreeze"));
        saveJob("비숍",                c.get("마법사"), s.get("pMage"),   s.get("aBuff"));
        saveJob("플레임 위자드",        c.get("마법사"), s.get("pMage"),   s.get("aBurst"));
        saveJob("에반",                c.get("마법사"), s.get("pMage"),   s.get("aBurst"));
        saveJob("루미너스",            c.get("마법사"), s.get("pMage"),   s.get("aFreeze"));
        saveJob("배틀메이지",          c.get("마법사"), s.get("pMage"),   s.get("aAoe"));
        saveJob("일리움",              c.get("마법사"), s.get("pMage"),   s.get("aBuff"));
        saveJob("라라",                c.get("마법사"), s.get("pPoison"),  s.get("aDotField"));
        saveJob("키네시스",             c.get("마법사"), s.get("pSlow"),   s.get("aSlowField"));

        // 궁수
        saveJob("보우마스터",       c.get("궁수"),  s.get("pArcher"),  s.get("aBurst"));
        saveJob("신궁",            c.get("궁수"),  s.get("pArcher"),  s.get("aAoe"));
        saveJob("패스파인더",       c.get("궁수"),  s.get("pPoison"),  s.get("aDotField"));
        saveJob("윈드브레이커",     c.get("궁수"),  s.get("pArcher"),  s.get("aAoe"));
        saveJob("메르세데스",       c.get("궁수"),  s.get("pArcher"),  s.get("aFreeze"));
        saveJob("와일드헌터",       c.get("궁수"),  s.get("pSlow"),   s.get("aSlowField"));
        saveJob("카인",            c.get("궁수"),  s.get("pArcher"),  s.get("aBurst"));

        // 도적
        saveJob("나이트로드",       c.get("도적"),  s.get("pThief"),  s.get("aBurst"));
        saveJob("섀도어",          c.get("도적"),  s.get("pPoison"),  s.get("aDotField"));
        saveJob("듀얼블레이더",     c.get("도적"),  s.get("pThief"),  s.get("aBurst"));
        saveJob("나이트워커",       c.get("도적"),  s.get("pSlow"),   s.get("aSlowField"));
        saveJob("괴도팬텀",         c.get("도적"),  s.get("pThief"),  s.get("aBurst"));
        saveJob("카데나",           c.get("도적"),  s.get("pPoison"),  s.get("aDotField"));
        saveJob("칼리",            c.get("도적"),  s.get("pMage"),   s.get("aAoe"));
        saveJob("호영",            c.get("도적"),  s.get("pThief"),  s.get("aAoe"));

        // 해적
        saveJob("바이퍼",          c.get("해적"),  s.get("pPirate"),  s.get("aBurst"));
        saveJob("캡틴",            c.get("해적"),  s.get("pArcher"),  s.get("aBurst"));
        saveJob("캐논슈터",         c.get("해적"),  s.get("pPirate"),  s.get("aAoe"));
        saveJob("스트라이커",       c.get("해적"),  s.get("pPirate"),  s.get("aAoe"));
        saveJob("은월",            c.get("해적"),  s.get("pPirate"),  s.get("aAoe"));
        saveJob("메카닉",          c.get("해적"),  s.get("pSlow"),   s.get("aSlowField"));
        saveJob("제논",            c.get("해적"),  s.get("pPirate"),  s.get("aBurst"));
        saveJob("엔젤릭버스터",     c.get("해적"),  s.get("pPirate"),  s.get("aAoe"));
        saveJob("아크",            c.get("해적"),  s.get("pPirate"),  s.get("aAoe"));
    }

    private void saveJob(String name, JobClass jc, Skill passive, Skill active) {
        jobRepository.save(Job.builder()
                .name(name).jobClass(jc)
                .passiveSkill(passive).activeSkill(active)
                .build());
    }
}