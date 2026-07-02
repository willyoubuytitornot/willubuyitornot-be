package com.willu.buyitornot.service;

import com.willu.buyitornot.exception.ResourceNotFoundException;
import com.willu.buyitornot.infra.collection.User;
import com.willu.buyitornot.infra.repository.UserRepository;
import com.willu.buyitornot.web.dto.request.CreateUserRequest;
import com.willu.buyitornot.web.dto.response.UserResponse;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * 컨트롤러 통합 테스트(UserControllerIntegrationTest)는 실제 UserService를 그대로 거치므로
 * new ObjectId(userId) 변환/findById 미존재 흐름은 실제 서비스 코드로 직접 검증한다
 * (design 01_design_user.md의 "잘못된 ObjectId 형식", "존재하지 않는 유저" 케이스).
 */
class UserServiceTest {

    @Test
    void createUser_저장후_UserResponse로_매핑() {
        UserRepository repo = mock(UserRepository.class);
        UserService service = new UserService(repo);

        User saved = new User("게이머");
        saved.setId(new ObjectId()); // 실제 저장 시 Mongo가 채우는 id를 모킹 환경에서 수동 부여
        given(repo.save(any(User.class))).willReturn(saved);

        UserResponse response = service.createUser(new CreateUserRequest("게이머"));

        assertThat(response.id()).isEqualTo(saved.getId().toHexString());
        assertThat(response.nickname()).isEqualTo("게이머");
        assertThat(response.createdAt()).isEqualTo(saved.getCreatedAt());
    }

    @Test
    void getUser_존재하면_UserResponse로_매핑() {
        UserRepository repo = mock(UserRepository.class);
        UserService service = new UserService(repo);

        User user = new User("홍길동");
        user.setId(new ObjectId());
        given(repo.findById(any(ObjectId.class))).willReturn(Optional.of(user));

        String hexId = new ObjectId().toHexString();
        UserResponse response = service.getUser(hexId);

        assertThat(response.nickname()).isEqualTo("홍길동");
    }

    @Test
    void getUser_존재하지않으면_ResourceNotFoundException() {
        UserRepository repo = mock(UserRepository.class);
        UserService service = new UserService(repo);

        given(repo.findById(any(ObjectId.class))).willReturn(Optional.empty());

        String hexId = new ObjectId().toHexString();

        assertThatThrownBy(() -> service.getUser(hexId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(hexId);
    }

    @Test
    void getUser_잘못된hex형식이면_IllegalArgumentException() {
        UserRepository repo = mock(UserRepository.class);
        UserService service = new UserService(repo);

        assertThatThrownBy(() -> service.getUser("not-a-valid-objectid"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
