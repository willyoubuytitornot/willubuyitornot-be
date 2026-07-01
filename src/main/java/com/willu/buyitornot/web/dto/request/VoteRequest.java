package com.willu.buyitornot.web.dto.request;

import com.willu.buyitornot.web.dto.Decision;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 투표 기록/갱신 요청. (POST /v1/users/{userId}/swipes/{swipeId}/votes)
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
