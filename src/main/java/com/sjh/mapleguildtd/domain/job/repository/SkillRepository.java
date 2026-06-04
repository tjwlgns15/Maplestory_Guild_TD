package com.sjh.mapleguildtd.domain.job.repository;

import com.sjh.mapleguildtd.domain.job.entity.Skill;
import com.sjh.mapleguildtd.domain.job.entity.SkillKind;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    List<Skill> findBySkillKind(SkillKind skillKind);
}

