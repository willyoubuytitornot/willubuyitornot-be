package com.willu.buyitornot.web.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

/**
 * 코드 기반 OpenAPI 명세의 전역 메타데이터(info/tags).
 * 각 operation의 태그 부여는 컨트롤러 메서드의 @Operation(tags=...)에서 수행한다.
 */
@OpenAPIDefinition(
        info = @Info(
                title = "게임 스와이프 API",
                version = "2.0.0",
                description = "스와이프 기반 게임 추천·투표 서비스 API. (users / swipes / games / user_swipes 4개 컬렉션)",
                contact = @Contact(name = "Game Swipe Team")
        ),
        tags = {
                @Tag(name = "Users", description = "유저 (users 컬렉션)"),
                @Tag(name = "Swipes", description = "주차별 스와이프 라운드 (swipes 컬렉션)"),
                @Tag(name = "Games", description = "게임 (games 컬렉션)"),
                @Tag(name = "Votes", description = "유저 투표 결과 (user_swipes 컬렉션)"),
                @Tag(name = "AI", description = "런타임 생성(비저장) — AI 한줄평/커뮤니티 요약/취향 리포트.")
        }
)
@Configuration
public class OpenApiConfig {
}
