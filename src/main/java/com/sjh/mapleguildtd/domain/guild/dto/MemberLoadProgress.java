package com.sjh.mapleguildtd.domain.guild.dto;

import com.sjh.mapleguildtd.domain.character.entity.CharacterCache;

/**
 * 길드원 로딩 진행 이벤트
 * SSE를 통해 프론트로 전달되는 단위 데이터
 */
public record MemberLoadProgress(
        int loaded,
        int total,
        CharacterCache character  // 방금 로딩된 캐릭터 (실패 시 null)
) {
    public boolean isDone() {
        return loaded == total;
    }
}
