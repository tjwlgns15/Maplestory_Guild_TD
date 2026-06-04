package com.sjh.mapleguildtd.domain.guild.client;

import com.sjh.mapleguildtd.infrastructure.client.dto.NexonApiResponse;
import com.sjh.mapleguildtd.infrastructure.exception.NexonApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class GuildApiClient {

    private final WebClient nexonWebClient;

    public NexonApiResponse.GuildId fetchGuildId(String apiKey, String guildName, String worldName) {
        return nexonWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/maplestory/v1/guild/id")
                        .queryParam("guild_name", guildName)
                        .queryParam("world_name", worldName)
                        .build())
                .header("x-nxopen-api-key", apiKey)
                .retrieve()
                .onStatus(HttpStatus.NOT_FOUND::equals,
                        response -> response.bodyToMono(String.class)
                                .map(body -> new NexonApiException("길드를 찾을 수 없습니다: " + guildName)))
                .onStatus(HttpStatus.TOO_MANY_REQUESTS::equals,
                        response -> response.bodyToMono(String.class)
                                .map(body -> new NexonApiException("API 요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.")))
                .bodyToMono(NexonApiResponse.GuildId.class)
                .block();
    }

    public NexonApiResponse.GuildBasic fetchGuildBasic(String apiKey, String oguildId) {
        return nexonWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/maplestory/v1/guild/basic")
                        .queryParam("oguild_id", oguildId)
                        .build())
                .header("x-nxopen-api-key", apiKey)
                .retrieve()
                .onStatus(HttpStatus.TOO_MANY_REQUESTS::equals,
                        response -> response.bodyToMono(String.class)
                                .map(body -> new NexonApiException("API 요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.")))
                .bodyToMono(NexonApiResponse.GuildBasic.class)
                .block();
    }
}