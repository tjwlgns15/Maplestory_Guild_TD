package com.sjh.mapleguildtd.domain.game.dto;

import com.sjh.mapleguildtd.domain.game.entity.GameResult;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

public class GameDto {

    @Getter
    public static class SaveRequest {
        private String playerName;
        private String guildName;
        private String worldName;
        private int clearRound;
        private List<String> selectedMembers; // 캐릭터명 목록
    }

    @Getter
    public static class RankingResponse {
        private final Long id;
        private final String playerName;
        private final String guildName;
        private final String worldName;
        private final int clearRound;
        private final LocalDateTime playedAt;

        private RankingResponse(Long id, String playerName, String guildName,
                                String worldName, int clearRound, LocalDateTime playedAt) {
            this.id = id;
            this.playerName = playerName;
            this.guildName = guildName;
            this.worldName = worldName;
            this.clearRound = clearRound;
            this.playedAt = playedAt;
        }

        public static RankingResponse from(GameResult result) {
            return new RankingResponse(
                    result.getId(),
                    result.getPlayerName(),
                    result.getGuildName(),
                    result.getWorldName(),
                    result.getClearRound(),
                    result.getPlayedAt()
            );
        }
    }
}