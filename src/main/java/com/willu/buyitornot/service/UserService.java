package com.willu.buyitornot.service;

import com.willu.buyitornot.exception.ResourceNotFoundException;
import com.willu.buyitornot.infra.collection.User;
import com.willu.buyitornot.infra.repository.UserRepository;
import com.willu.buyitornot.web.dto.request.CreateUserRequest;
import com.willu.buyitornot.web.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserResponse createUser(CreateUserRequest request) {
        User saved = userRepository.save(new User(request.nickname()));
        return toResponse(saved);
    }

    public UserResponse getUser(String userId) {
        ObjectId objectId = new ObjectId(userId);
        User user = userRepository.findById(objectId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return toResponse(user);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId().toHexString(), user.getNickname(), user.getCreatedAt());
    }
}
