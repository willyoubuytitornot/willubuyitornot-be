package com.willu.buyitornot.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * AI 취향 리포트. user_swipes 집계 + 장르 + AI 페르소나. 요청 시점에 생성하며 DB에 저장하지 않는다.
 */
public record PreferenceReportResponse(

        @Schema(description = "확인한 총 게임 수 (buy+skip+maybe)", example = "10")
        Integer totalCount,

        @Schema(description = "buy 수", example = "4")
        Integer buyCount,

        @Schema(description = "skip 수", example = "4")
        Integer skipCount,

        @Schema(description = "maybe 수", example = "2")
        Integer maybeCount,

        @Schema(description = "buy한 게임의 장르 비율(내림차순)")
        List<GenreStatDto> genreStats,

        @Schema(description = "스와이프 패턴 기반 AI 페르소나")
        PersonaDto persona
) {
}
