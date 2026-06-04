package com.sjh.mapleguildtd.domain.guild.service;

import com.sjh.mapleguildtd.domain.guild.client.GuildApiClient;
import com.sjh.mapleguildtd.domain.guild.entity.GuildCache;
import com.sjh.mapleguildtd.domain.guild.entity.GuildMember;
import com.sjh.mapleguildtd.domain.guild.repository.GuildCacheRepository;
import com.sjh.mapleguildtd.infrastructure.client.dto.NexonApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GuildCacheService {

    private final GuildCacheRepository guildCacheRepository;
    private final GuildApiClient guildApiClient;

    /**
     * 길드 캐시 조회
     * - 캐시 존재 + 유효 → DB에서 반환
     * - 캐시 없음 또는 만료 → 넥슨 API 호출 후 저장/갱신
     */
    @Transactional
    public GuildCache getOrFetch(String apiKey, String guildName, String worldName) {
        return guildCacheRepository.findByGuildNameAndWorldName(guildName, worldName)
                .map(cache -> refreshIfExpired(apiKey, cache))
                .orElseGet(() -> fetchAndSave(apiKey, guildName, worldName));
    }

    @Transactional(readOnly = true)
    public List<String> getMemberNames(String apiKey, String guildName, String worldName) {
        GuildCache cache = getOrFetch(apiKey, guildName, worldName);
        // 트랜잭션 안에서 members 컬렉션까지 읽어 String 목록으로 변환
        return cache.getMembers().stream()
                .map(GuildMember::getCharacterName)
                .toList();
    }

    private GuildCache refreshIfExpired(String apiKey, GuildCache cache) {
        if (!cache.isExpired()) {
            return cache;
        }
        // 만료된 경우 삭제 후 재조회 (길드원 목록이 바뀔 수 있으므로 전체 갱신)
        guildCacheRepository.delete(cache);
        return fetchAndSave(apiKey, cache.getGuildName(), cache.getWorldName());
    }

    private GuildCache fetchAndSave(String apiKey, String guildName, String worldName) {
        NexonApiResponse.GuildId guildId = guildApiClient.fetchGuildId(apiKey, guildName, worldName);
        NexonApiResponse.GuildBasic guildBasic = guildApiClient.fetchGuildBasic(apiKey, guildId.getOguildId());

        GuildCache guildCache = GuildCache.builder()
                .guildName(guildBasic.getGuildName())
                .worldName(guildBasic.getWorldName())
                .oguildId(guildId.getOguildId())
                .masterName(guildBasic.getGuildMasterName())
                .guildLevel(guildBasic.getGuildLevel())
                .memberCount(guildBasic.getGuildMemberCount())
                .build();

        guildBasic.getGuildMember().forEach(characterName -> {
            GuildMember member = GuildMember.builder()
                    .guildCache(guildCache)
                    .characterName(characterName)
                    .build();
            guildCache.addMember(member);
        });

        return guildCacheRepository.save(guildCache);
    }
}