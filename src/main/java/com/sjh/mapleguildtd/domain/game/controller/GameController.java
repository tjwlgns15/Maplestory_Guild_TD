package com.sjh.mapleguildtd.domain.game.controller;

import com.sjh.mapleguildtd.domain.game.dto.GameDto;
import com.sjh.mapleguildtd.domain.game.entity.GameResult;
import com.sjh.mapleguildtd.domain.game.repository.GameResultRepository;
import com.sjh.mapleguildtd.infrastructure.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor
public class GameController {

    private final GameResultRepository gameResultRepository;
    private final ObjectMapper objectMapper;

    /**
     * 게임 결과 저장
     */
    @PostMapping("/result")
    public ResponseEntity<ApiResponse<Long>> saveResult(
            @RequestBody GameDto.SaveRequest request) {

        String selectedMembersJson = objectMapper.writeValueAsString(request.getSelectedMembers());

        GameResult result = GameResult.builder()
                .playerName(request.getPlayerName())
                .guildName(request.getGuildName())
                .worldName(request.getWorldName())
                .clearRound(request.getClearRound())
                .selectedMembers(selectedMembersJson)
                .build();

        GameResult saved = gameResultRepository.save(result);
        return ResponseEntity.ok(ApiResponse.ok(saved.getId()));
    }

    /**
     * 랭킹 조회 (클리어 라운드 내림차순 Top 10)
     */
    @GetMapping("/ranking")
    public ResponseEntity<ApiResponse<List<GameDto.RankingResponse>>> getRanking() {
        List<GameDto.RankingResponse> ranking = gameResultRepository
                .findTop10ByOrderByClearRoundDescPlayedAtAsc()
                .stream()
                .map(GameDto.RankingResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(ranking));
    }
}