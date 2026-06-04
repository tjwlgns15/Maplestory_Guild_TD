package com.sjh.mapleguildtd.domain.guild.service;

import com.sjh.mapleguildtd.domain.character.entity.CharacterCache;
import com.sjh.mapleguildtd.domain.character.service.CharacterCacheService;
import com.sjh.mapleguildtd.domain.guild.dto.MemberLoadProgress;
import com.sjh.mapleguildtd.domain.guild.entity.GuildCache;
import com.sjh.mapleguildtd.domain.guild.entity.GuildMember;
import com.sjh.mapleguildtd.infrastructure.exception.NexonApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuildMemberLoadService {

    private static final int BATCH_DELAY_MS = 120;

    private final CharacterCacheService characterCacheService;
    private final GuildCacheService guildCacheService;

    /**
     * 길드원 전체 스탯 로딩
     *
     * @param apiKey     넥슨 API Key
     * @param guildName  길드명
     * @param worldName  월드명
     * @param onProgress 진행 콜백 → SSE 이벤트 전송에 사용
     * @return 로딩된 캐릭터 캐시 목록
     */
    public List<CharacterCache> loadAllMembers(String apiKey, String guildName,
                                               String worldName,
                                               Consumer<MemberLoadProgress> onProgress) {
        List<String> memberNames = guildCacheService.getMemberNames(apiKey, guildName, worldName);

        int total = memberNames.size();
        List<CharacterCache> results = new ArrayList<>();

        for (int i = 0; i < total; i++) {
            String characterName = memberNames.get(i);
            CharacterCache loaded = null;
            try {
                loaded = characterCacheService.getOrFetch(apiKey, characterName);
                results.add(loaded);
            } catch (NexonApiException e) {
                log.warn("캐릭터 조회 실패 - 건너뜀: {} / 사유: {}", characterName, e.getMessage());
            }

            onProgress.accept(new MemberLoadProgress(i + 1, total, loaded));
            applyRateLimit();
        }

        return results;
    }

    private void applyRateLimit() {
        try {
            Thread.sleep(BATCH_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}