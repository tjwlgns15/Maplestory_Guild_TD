package com.sjh.mapleguildtd.domain.character.service;

import com.sjh.mapleguildtd.domain.character.client.CharacterApiClient;
import com.sjh.mapleguildtd.domain.character.entity.CharacterCache;
import com.sjh.mapleguildtd.domain.character.repository.CharacterCacheRepository;
import com.sjh.mapleguildtd.infrastructure.client.dto.NexonApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CharacterCacheService {

    private final CharacterCacheRepository characterCacheRepository;
    private final CharacterApiClient characterApiClient;

    /**
     * 캐릭터 캐시 조회
     * - 캐시 존재 + 유효 → DB에서 반환
     * - 캐시 없음 또는 만료 → 넥슨 API 호출 후 저장/갱신
     */
    @Transactional
    public CharacterCache getOrFetch(String apiKey, String characterName) {
        return characterCacheRepository.findByCharacterName(characterName)
                .map(cache -> refreshIfExpired(apiKey, cache))
                .orElseGet(() -> fetchAndSave(apiKey, characterName));
    }

    private CharacterCache refreshIfExpired(String apiKey, CharacterCache cache) {
        if (!cache.isExpired()) {
            return cache;
        }
        NexonApiResponse.CharacterBasic basic = characterApiClient.fetchCharacterBasic(apiKey, cache.getOcid());
        NexonApiResponse.CharacterStat stat = characterApiClient.fetchCharacterStat(apiKey, cache.getOcid());

        cache.refresh(
                basic.getCharacterClass(),
                basic.getCharacterLevel(),
                stat.extractCombatPower(),
                basic.getCharacterImage()
        );
        return cache;
    }

    private CharacterCache fetchAndSave(String apiKey, String characterName) {
        NexonApiResponse.CharacterId characterId = characterApiClient.fetchCharacterId(apiKey, characterName);
        String ocid = characterId.getOcid();

        NexonApiResponse.CharacterBasic basic = characterApiClient.fetchCharacterBasic(apiKey, ocid);
        NexonApiResponse.CharacterStat stat = characterApiClient.fetchCharacterStat(apiKey, ocid);

        CharacterCache cache = CharacterCache.builder()
                .characterName(characterName)
                .ocid(ocid)
                .job(basic.getCharacterClass())
                .level(basic.getCharacterLevel())
                .combatPower(stat.extractCombatPower())
                .characterImage(basic.getCharacterImage())
                .build();

        return characterCacheRepository.save(cache);
    }
}