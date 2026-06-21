package com.willu.buyitornot.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * swipes 컬렉션 문서 + gameIdList를 확장한 game 문서 목록(조인 결과).
 */
public record SwipeWithGamesResponse(

        @Schema(description = "swipes._id (ObjectId)", example = "65a1f2c3d4e5f6a7b8c9d0e1")
        String id,

        @Schema(description = "이 라운드에 포함된 게임 ID 목록")
        List<String> gameIdList,

        @Schema(description = "스와이프 생성 시각 (reset 기준 시각)", example = "2025-01-04T12:00:00")
        LocalDateTime createdAt,

        @Schema(description = "gameIdList에 대응하는 게임 상세 목록 (조인 결과)")
        List<GameResponse> games
) {
}
