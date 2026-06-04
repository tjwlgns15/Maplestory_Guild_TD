package com.sjh.mapleguildtd.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

public class NexonApiResponse {

    // ─── 길드 식별자 조회 응답 ───────────────────────────────
    @Getter
    public static class GuildId {
        @JsonProperty("oguild_id")
        private String oguildId;
    }

    // ─── 길드 기본 정보 조회 응답 ────────────────────────────
    @Getter
    public static class GuildBasic {
        @JsonProperty("world_name")
        private String worldName;

        @JsonProperty("guild_name")
        private String guildName;

        @JsonProperty("guild_level")
        private int guildLevel;

        @JsonProperty("guild_master_name")
        private String guildMasterName;

        @JsonProperty("guild_member_count")
        private int guildMemberCount;

        @JsonProperty("guild_member")
        private List<String> guildMember;
    }

    // ─── 캐릭터 식별자 조회 응답 ─────────────────────────────
    @Getter
    public static class CharacterId {
        @JsonProperty("ocid")
        private String ocid;
    }

    // ─── 캐릭터 기본 정보 조회 응답 ──────────────────────────
    @Getter
    public static class CharacterBasic {
        @JsonProperty("character_name")
        private String characterName;

        @JsonProperty("character_class")
        private String characterClass;

        @JsonProperty("character_level")
        private int characterLevel;

        @JsonProperty("character_image")
        private String characterImage;
    }

    // ─── 캐릭터 스탯 조회 응답 ───────────────────────────────
    @Getter
    public static class CharacterStat {
        @JsonProperty("final_stat")
        private List<StatDetail> finalStat;

        @Getter
        public static class StatDetail {
            @JsonProperty("stat_name")
            private String statName;

            @JsonProperty("stat_value")
            private String statValue;
        }

        public long extractCombatPower() {
            return finalStat.stream()
                    .filter(stat -> "전투력".equals(stat.getStatName()))
                    .findFirst()
                    .map(stat -> Long.parseLong(stat.getStatValue()))
                    .orElse(0L);
        }
    }
}