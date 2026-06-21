package com.willu.buyitornot.web.ui;

import com.willu.buyitornot.service.SwipeService;
import com.willu.buyitornot.web.dto.response.SwipeWithGamesResponse;
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
@RequestMapping("/v1/swipes")
@RequiredArgsConstructor
public class SwipeController {

    private final SwipeService swipeService;

    @Operation(
            summary = "현재 활성 스와이프 라운드 조회",
            description = "가장 최근(현재 주차) Swipe 문서를 반환. gameIdList에 해당하는 game 문서를 games로 확장해 포함.",
            tags = {"Swipes"}
    )
    @GetMapping("/current")
    public SwipeWithGamesResponse current() {
        return swipeService.getCurrent();
    }

    @Operation(summary = "특정 스와이프 라운드 조회", tags = {"Swipes"})
    @GetMapping("/{swipeId}")
    public SwipeWithGamesResponse get(
            @Parameter(description = "swipes._id (ObjectId)") @PathVariable String swipeId) {
        return swipeService.getById(swipeId);
    }
}
