package com.sjh.mapleguildtd.domain.character.client;

import com.sjh.mapleguildtd.infrastructure.client.dto.NexonApiResponse;
import com.sjh.mapleguildtd.infrastructure.exception.NexonApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class CharacterApiClient {

    private final WebClient nexonWebClient;

    public NexonApiResponse.CharacterId fetchCharacterId(String apiKey, String characterName) {
        return nexonWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/maplestory/v1/id")
                        .queryParam("character_name", characterName)
                        .build())
                .header("x-nxopen-api-key", apiKey)
                .retrieve()
                .onStatus(HttpStatus.NOT_FOUND::equals,
                        response -> response.bodyToMono(String.class)
                                .map(body -> new NexonApiException("캐릭터를 찾을 수 없습니다: " + characterName)))
                .onStatus(HttpStatus.TOO_MANY_REQUESTS::equals,
                        response -> response.bodyToMono(String.class)
                                .map(body -> new NexonApiException("API 요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.")))
                .bodyToMono(NexonApiResponse.CharacterId.class)
                .block();
    }

    public NexonApiResponse.CharacterBasic fetchCharacterBasic(String apiKey, String ocid) {
        return nexonWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/maplestory/v1/character/basic")
                        .queryParam("ocid", ocid)
                        .build())
                .header("x-nxopen-api-key", apiKey)
                .retrieve()
                .onStatus(HttpStatus.TOO_MANY_REQUESTS::equals,
                        response -> response.bodyToMono(String.class)
                                .map(body -> new NexonApiException("API 요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.")))
                .bodyToMono(NexonApiResponse.CharacterBasic.class)
                .block();
    }

    public NexonApiResponse.CharacterStat fetchCharacterStat(String apiKey, String ocid) {
        return nexonWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/maplestory/v1/character/stat")
                        .queryParam("ocid", ocid)
                        .build())
                .header("x-nxopen-api-key", apiKey)
                .retrieve()
                .onStatus(HttpStatus.TOO_MANY_REQUESTS::equals,
                        response -> response.bodyToMono(String.class)
                                .map(body -> new NexonApiException("API 요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.")))
                .bodyToMono(NexonApiResponse.CharacterStat.class)
                .block();
    }
}