package com.willu.buyitornot.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 유저 생성 요청. (POST /v1/users)
 */
public record CreateUserRequest(

        @Schema(description = "유저 닉네임", example = "게이머", maxLength = 12)
        @NotBlank
        @Size(max = 12)
        String nickname
) {
}
