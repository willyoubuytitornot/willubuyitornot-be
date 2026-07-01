package com.willu.buyitornot.web.dto.request;

import com.willu.buyitornot.web.dto.Decision;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 배치 투표 요청의 단일 항목. (POST /v1/users/{userId}/swipes/{swipeId}/votes 는 이 항목들의 배열을 받는다)
 */
public record VoteRequest(

        @Schema(description = "게임 ObjectId(24자리 16진수)", example = "65a1f2c3d4e5f6a7b8c9d0e1")
        @NotBlank
        String gameId,

        @Schema(description = "투표 결정")
        @NotNull
        Decision decision
) {
}
