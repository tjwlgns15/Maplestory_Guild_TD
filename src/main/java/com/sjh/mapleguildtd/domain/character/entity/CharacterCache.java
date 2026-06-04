package com.sjh.mapleguildtd.domain.character.entity;

import com.sjh.mapleguildtd.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "character_cache",
        indexes = @Index(name = "idx_character_name", columnList = "character_name"))
public class CharacterCache extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String characterName;

    @Column(nullable = false, length = 100)
    private String ocid;  // 캐릭터 당 고유 id

    @Column(nullable = false, length = 50)
    private String job;

    @Column(nullable = false)
    private int level;

    @Column(nullable = false)
    private long combatPower;

    @Column(columnDefinition = "TEXT")
    private String characterImage;

    @Column(nullable = false)
    private LocalDateTime cachedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Builder
    private CharacterCache(String characterName, String ocid, String job,
                           int level, long combatPower, String characterImage) {
        this.characterName = characterName;
        this.ocid = ocid;
        this.job = job;
        this.level = level;
        this.combatPower = combatPower;
        this.characterImage = characterImage;
        this.cachedAt = LocalDateTime.now();
        this.expiresAt = this.cachedAt.plusHours(24);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public void refresh(String job, int level, long combatPower, String characterImage) {
        this.job = job;
        this.level = level;
        this.combatPower = combatPower;
        this.characterImage = characterImage;
        this.cachedAt = LocalDateTime.now();
        this.expiresAt = this.cachedAt.plusHours(24);
    }
}