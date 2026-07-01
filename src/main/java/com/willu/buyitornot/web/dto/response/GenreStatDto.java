package com.willu.buyitornot.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 취향 리포트의 장르별 통계.
 */
public record GenreStatDto(

        @Schema(description = "장르", example = "RPG")
        String genre,

        @Schema(description = "게임 수", example = "3")
        Integer count,

        @Schema(description = "비율(%)", example = "60")
        Integer percentage
) {
}
