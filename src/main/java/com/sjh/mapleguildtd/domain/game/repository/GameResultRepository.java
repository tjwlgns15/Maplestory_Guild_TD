package com.sjh.mapleguildtd.domain.game.repository;

import com.sjh.mapleguildtd.domain.game.entity.GameResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameResultRepository extends JpaRepository<GameResult, Long> {

    List<GameResult> findTop10ByOrderByClearRoundDescPlayedAtAsc();
}