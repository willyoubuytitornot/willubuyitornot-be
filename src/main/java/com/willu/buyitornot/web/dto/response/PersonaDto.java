package com.willu.buyitornot.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 스와이프 패턴 기반 AI 페르소나.
 */
public record PersonaDto(

        @Schema(description = "페르소나 타이틀", example = "신중한 세계관 탐험가")
        String title,

        @Schema(description = "페르소나 설명", example = "깊은 서사와 선택의 무게를 음미하는 당신. 한 편의 이야기를 끝까지 파고드는 몰입형 플레이어예요.")
        String description,

        @Schema(description = "태그 목록", example = "[\"#스토리덕후\", \"#세계관탐험\", \"#엔딩수집가\"]")
        List<String> tags
) {
}
