package com.sjh.mapleguildtd.domain.guild.dto;

import com.sjh.mapleguildtd.domain.character.dto.CharacterCacheResponse;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberLoadEventResponse {

    private final int loaded;
    private final int total;
    private final boolean done;
    private final CharacterCacheResponse member; // 실패 시 null

    public static MemberLoadEventResponse from(MemberLoadProgress progress) {
        CharacterCacheResponse memberResponse = progress.character() != null
                ? CharacterCacheResponse.from(progress.character())
                : null;

        return new MemberLoadEventResponse(
                progress.loaded(),
                progress.total(),
                progress.isDone(),
                memberResponse
        );
    }
}