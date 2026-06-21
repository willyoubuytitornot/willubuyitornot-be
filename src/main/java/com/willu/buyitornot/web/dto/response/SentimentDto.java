package com.willu.buyitornot.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 게임 인사이트의 항목별 평가 점수.
 */
public record SentimentDto(

        @Schema(description = "평가 항목 라벨", example = "스토리")
        String label,

        @Schema(description = "점수(0~100)", example = "91")
        Integer score
) {
}
