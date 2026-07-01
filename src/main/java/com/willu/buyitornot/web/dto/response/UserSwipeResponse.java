package com.willu.buyitornot.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * user_swipes 컬렉션 문서 응답. 한 유저의 한 Swipe에 대한 투표 결과를 buy/skip/maybe 배열로 집계.
 */
public record UserSwipeResponse(

        @Schema(description = "user_swipes._id (ObjectId)", example = "65a1f2c3d4e5f6a7b8c9d0e1")
        String id,

        @Schema(description = "유저 ID", example = "65a1f2c3d4e5f6a7b8c9d0e1")
        String userId,

        @Schema(description = "스와이프 ID", example = "65a1f2c3d4e5f6a7b8c9d0e1")
        String swipeId,

        @Schema(description = "buy(구매)로 투표한 게임 ID 목록")
        List<String> buy,

        @Schema(description = "skip(패스)로 투표한 게임 ID 목록")
        List<String> skip,

        @Schema(description = "maybe(고민중)로 투표한 게임 ID 목록")
        List<String> maybe,

        @Schema(description = "생성 시각", example = "2025-01-04T12:10:00")
        LocalDateTime createdAt,

        @Schema(description = "수정 시각", example = "2025-01-04T12:15:22")
        LocalDateTime updatedAt
) {
}
