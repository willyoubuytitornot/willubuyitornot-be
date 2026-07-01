package com.willu.buyitornot.web.ui;

import com.willu.buyitornot.service.UserSwipeService;
import com.willu.buyitornot.web.dto.request.VoteRequest;
import com.willu.buyitornot.web.dto.response.PreferenceReportResponse;
import com.willu.buyitornot.web.dto.response.UserSwipeResponse;
import com.willu.buyitornot.web.ui.common.WrapResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@WrapResponse
@RestController
@RequestMapping("/v1/users/{userId}/swipes/{swipeId}")
@RequiredArgsConstructor
public class UserSwipeController {

    private final UserSwipeService userSwipeService;

    @Operation(
            summary = "유저의 스와이프 결과 조회",
            description = "user_swipes에서 (userId, swipeId)로 단일 문서를 조회. 아직 참여 전이면 빈 배열.",
            tags = {"Votes"}
    )
    @GetMapping
    public UserSwipeResponse getResult(
            @Parameter(description = "users._id (ObjectId)") @PathVariable String userId,
            @Parameter(description = "swipes._id (ObjectId)") @PathVariable String swipeId) {
        return userSwipeService.getResult(userId, swipeId);
    }

    @Operation(
            summary = "투표 기록 / 갱신 (배치)",
            description = "n개 게임에 대한 결정을 한 번에 저장. (userId, swipeId) user_swipes 문서를 upsert.",
            tags = {"Votes"}
    )
    @PostMapping("/votes")
    public UserSwipeResponse vote(
            @Parameter(description = "users._id (ObjectId)") @PathVariable String userId,
            @Parameter(description = "swipes._id (ObjectId)") @PathVariable String swipeId,
            @Valid @RequestBody List<VoteRequest> requests) {
        return userSwipeService.vote(userId, swipeId, requests);
    }

    @Operation(
            summary = "AI 취향 리포트 조회 (런타임 생성)",
            description = "user_swipes 집계 + games 장르를 바탕으로 장르 선호도와 AI 페르소나를 생성해 반환. DB에 저장하지 않음.",
            tags = {"AI"}
    )
    @GetMapping("/report")
    public PreferenceReportResponse report(
            @Parameter(description = "users._id (ObjectId)") @PathVariable String userId,
            @Parameter(description = "swipes._id (ObjectId)") @PathVariable String swipeId) {
        return userSwipeService.getReport(userId, swipeId);
    }
}
