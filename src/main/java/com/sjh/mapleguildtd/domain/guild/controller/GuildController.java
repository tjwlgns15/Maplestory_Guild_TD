package com.sjh.mapleguildtd.domain.guild.controller;

import com.sjh.mapleguildtd.domain.guild.dto.GuildInfoResponse;
import com.sjh.mapleguildtd.domain.guild.dto.MemberLoadEventResponse;
import com.sjh.mapleguildtd.domain.guild.entity.GuildCache;
import com.sjh.mapleguildtd.domain.guild.service.GuildCacheService;
import com.sjh.mapleguildtd.domain.guild.service.GuildMemberLoadService;
import com.sjh.mapleguildtd.infrastructure.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RestController
@RequestMapping("/api/guild")
@RequiredArgsConstructor
public class GuildController {

    private final GuildCacheService guildCacheService;
    private final GuildMemberLoadService guildMemberLoadService;
    private final ObjectMapper objectMapper;

    /**
     * 길드 기본 정보 조회
     * 길드원 목록 로딩 전 길드 존재 여부 및 기본 정보 확인용
     */
    @GetMapping("/info")
    public ResponseEntity<ApiResponse<GuildInfoResponse>> getGuildInfo(
            @RequestParam String apiKey,
            @RequestParam String guildName,
            @RequestParam String worldName) {

        GuildCache guildCache = guildCacheService.getOrFetch(apiKey, guildName, worldName);
        return ResponseEntity.ok(ApiResponse.ok(GuildInfoResponse.from(guildCache)));
    }

    /**
     * 길드원 스탯 로딩 SSE 스트림
     * 클라이언트는 이 엔드포인트를 구독하여 진행률과 캐릭터 데이터를 실시간으로 수신
     *
     * SSE 이벤트 구조:
     * - event: "member"  → 캐릭터 1명 로딩 완료 (MemberLoadEventResponse)
     * - event: "error"   → 전체 로딩 실패
     */
    @GetMapping(value = "/members/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamGuildMembers(
            @RequestParam String apiKey,
            @RequestParam String guildName,
            @RequestParam String worldName) {

        SseEmitter emitter = new SseEmitter(300_000L); // 5분 타임아웃

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                guildMemberLoadService.loadAllMembers(apiKey, guildName, worldName, progress -> {
                    try {
                        MemberLoadEventResponse event = MemberLoadEventResponse.from(progress);
                        emitter.send(SseEmitter.event()
                                .name("member")
                                .data(objectMapper.writeValueAsString(event)));
                    } catch (IOException e) {
                        log.error("SSE 전송 실패", e);
                        emitter.completeWithError(e);
                    }
                });
                emitter.complete();
            } catch (Exception e) {
                log.error("길드원 로딩 중 오류 발생", e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(e.getMessage()));
                } catch (IOException ignored) {}
                emitter.completeWithError(e);
            } finally {
                executor.shutdown();
            }
        });

        return emitter;
    }
}