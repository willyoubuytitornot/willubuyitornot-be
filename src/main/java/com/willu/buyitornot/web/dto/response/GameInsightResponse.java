package com.willu.buyitornot.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 게임의 AI 인사이트. 요청 시점에 생성하며 DB에 저장하지 않는다(런타임 생성).
 */
public record GameInsightResponse(

        @Schema(description = "게임 ID", example = "65a1f2c3d4e5f6a7b8c9d0e1")
        String gameId,

        @Schema(description = "별점(런타임 산출, 0~5)", example = "4.7")
        Float rating,

        @Schema(description = "리뷰 수", example = "1842")
        Integer reviews,

        @Schema(description = "긍정 비율(%)", example = "84")
        Integer positive,

        @Schema(description = "부정 비율(%) = 100 - positive", example = "16")
        Integer negative,

        @Schema(description = "AI 커뮤니티 요약", example = "방대한 세계관과 자유도 높은 탐험을 호평하는 리뷰가 많아요.")
        String summary,

        @Schema(description = "항목별 평가 점수")
        List<SentimentDto> sentiments,

        @Schema(description = "장점 목록", example = "[\"깊은 서사\", \"광활한 탐험\"]")
        List<String> pros,

        @Schema(description = "단점 목록", example = "[\"느린 초반\"]")
        List<String> cons
) {
}
