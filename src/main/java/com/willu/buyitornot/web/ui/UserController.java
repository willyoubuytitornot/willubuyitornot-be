package com.willu.buyitornot.web.ui;

import com.willu.buyitornot.service.UserService;
import com.willu.buyitornot.web.dto.request.CreateUserRequest;
import com.willu.buyitornot.web.dto.response.UserResponse;
import com.willu.buyitornot.web.ui.common.WrapResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@WrapResponse
@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "유저 생성", description = "온보딩에서 닉네임으로 유저 생성. users 컬렉션에 문서 추가.", tags = {"Users"})
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }

    @Operation(summary = "유저 조회", tags = {"Users"})
    @GetMapping("/{userId}")
    public UserResponse get(
            @Parameter(description = "users._id (ObjectId)") @PathVariable String userId) {
        return userService.getUser(userId);
    }
}
