package com.sjh.mapleguildtd.domain.character.dto;

import com.sjh.mapleguildtd.domain.character.entity.CharacterCache;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CharacterCacheResponse {

    private final String characterName;
    private final String job;
    private final int level;
    private final long combatPower;
    private final String characterImage;

    public static CharacterCacheResponse from(CharacterCache cache) {
        return new CharacterCacheResponse(
                cache.getCharacterName(),
                cache.getJob(),
                cache.getLevel(),
                cache.getCombatPower(),
                cache.getCharacterImage()
        );
    }
}