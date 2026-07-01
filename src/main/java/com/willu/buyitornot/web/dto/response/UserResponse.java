package com.willu.buyitornot.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * users 컬렉션 문서 응답.
 */
public record UserResponse(

        @Schema(description = "users._id (ObjectId)", example = "65a1f2c3d4e5f6a7b8c9d0e1")
        String id,

        @Schema(description = "닉네임", example = "홍길동")
        String nickname,

        @Schema(description = "생성 시각", example = "2025-01-04T12:00:00")
        LocalDateTime createdAt
) {
}
