package com.willu.buyitornot.web.ui;

import com.willu.buyitornot.service.GameService;
import com.willu.buyitornot.web.dto.response.GameInsightResponse;
import com.willu.buyitornot.web.dto.response.GameResponse;
import com.willu.buyitornot.web.ui.common.WrapResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@WrapResponse
@RestController
@RequestMapping("/v1/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @Operation(summary = "게임 상세 조회", description = "games 컬렉션의 단일 게임.", tags = {"Games"})
    @GetMapping("/{gameId}")
    public GameResponse get(
            @Parameter(description = "games._id (ObjectId)") @PathVariable String gameId) {
        return gameService.getGame(gameId);
    }

    @Operation(
            summary = "게임 AI 인사이트 조회 (런타임 생성)",
            description = "AI 커뮤니티 요약·긍/부정·세부 평가·장단점을 요청 시점에 생성해 반환. DB에 저장하지 않음.",
            tags = {"AI"}
    )
    @GetMapping("/{gameId}/insight")
    public GameInsightResponse insight(
            @Parameter(description = "games._id (ObjectId)") @PathVariable String gameId) {
        return gameService.getInsight(gameId);
    }
}
