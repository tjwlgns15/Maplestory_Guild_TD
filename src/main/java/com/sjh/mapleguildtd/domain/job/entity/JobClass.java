package com.sjh.mapleguildtd.domain.job.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "job_class")
public class JobClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 직업군 이름 (전사, 마법사, 궁수, 도적, 해적)
    @Column(nullable = false, unique = true, length = 20)
    private String name;

    // 기본 사거리 (px 기준)
    @Column(name = "attack_range", nullable = false)
    private int range;

    // 공격 쿨다운 (ms)
    @Column(nullable = false)
    private int cooldown;

    // 타워 표시 색상 (hex)
    @Column(nullable = false, length = 10)
    private String color;

    @OneToMany(mappedBy = "jobClass", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Job> jobs = new ArrayList<>();

    @Builder
    private JobClass(String name, int range, int cooldown, String color) {
        this.name     = name;
        this.range    = range;
        this.cooldown = cooldown;
        this.color    = color;
    }

    public void update(int range, int cooldown, String color) {
        this.range    = range;
        this.cooldown = cooldown;
        this.color    = color;
    }
}