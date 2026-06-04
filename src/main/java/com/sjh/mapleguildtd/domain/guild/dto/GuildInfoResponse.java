package com.sjh.mapleguildtd.domain.guild.dto;

import com.sjh.mapleguildtd.domain.guild.entity.GuildCache;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class GuildInfoResponse {

    private final String guildName;
    private final String worldName;
    private final String masterName;
    private final int guildLevel;
    private final int memberCount;

    public static GuildInfoResponse from(GuildCache cache) {
        return new GuildInfoResponse(
                cache.getGuildName(),
                cache.getWorldName(),
                cache.getMasterName(),
                cache.getGuildLevel(),
                cache.getMemberCount()
        );
    }
}
