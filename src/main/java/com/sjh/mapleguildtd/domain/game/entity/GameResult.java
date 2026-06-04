package com.sjh.mapleguildtd.domain.game.entity;

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
@Table(name = "game_result",
        indexes = @Index(name = "idx_clear_round", columnList = "clear_round DESC"))
public class GameResult extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String playerName;

    @Column(nullable = false, length = 50)
    private String guildName;

    @Column(nullable = false, length = 20)
    private String worldName;

    @Column(nullable = false)
    private int clearRound;

    // 선택한 길드원 목록 - JSON 배열로 저장 ex) ["캐릭터A","캐릭터B","캐릭터C"]
    @Column(nullable = false, columnDefinition = "TEXT")
    private String selectedMembers;

    @Column(nullable = false)
    private LocalDateTime playedAt;

    @Builder
    private GameResult(String playerName, String guildName, String worldName,
                       int clearRound, String selectedMembers) {
        this.playerName = playerName;
        this.guildName = guildName;
        this.worldName = worldName;
        this.clearRound = clearRound;
        this.selectedMembers = selectedMembers;
        this.playedAt = LocalDateTime.now();
    }
}