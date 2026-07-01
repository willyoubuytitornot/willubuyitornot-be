package com.willu.buyitornot.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * games 컬렉션 문서 응답.
 */
public record GameResponse(

        @Schema(description = "games._id (ObjectId)", example = "65a1f2c3d4e5f6a7b8c9d0e1")
        String id,

        @Schema(description = "게임 썸네일 이미지 URL", example = "https://example.com/game.jpg")
        String imageUrl,

        @Schema(description = "장르(자유 문자열)", example = "RPG")
        String genre,

        @Schema(description = "출시일", example = "2024-03-15")
        LocalDate releaseDate,

        @Schema(description = "게임 제목/설명", example = "모험을 떠나는 RPG")
        String title,

        @Schema(description = "스와이프 카드용 AI 한줄평", example = "초반은 잔잔하지만 후반 서사가 묵직하게 몰아쳐요")
        String aiComment,

        @Schema(description = "스토어 페이지 URL", example = "https://store.onstove.com/games/g1")
        String storeUrl,

        @Schema(description = "커뮤니티 페이지 URL", example = "https://community.onstove.com/games/g1")
        String communityUrl,

        @Schema(description = "생성 시각", example = "2025-01-01T00:00:00")
        LocalDateTime createdAt,

        @Schema(description = "수정 시각", example = "2025-01-01T00:00:00")
        LocalDateTime updatedAt
) {
}
