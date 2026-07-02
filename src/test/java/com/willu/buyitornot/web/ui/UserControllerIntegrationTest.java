package com.willu.buyitornot.web.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.willu.buyitornot.infra.collection.User;
import com.willu.buyitornot.infra.repository.UserRepository;
import com.willu.buyitornot.support.AbstractMongoIntegrationTest;
import com.willu.buyitornot.web.dto.request.CreateUserRequest;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UserController(web.ui) + UserService(service) — Testcontainers MongoDB에 연결된 전체 컨텍스트로
 * 실제 서비스/리포지토리 계층까지 거쳐 검증한다(모킹하지 않음).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class UserControllerIntegrationTest extends AbstractMongoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    // ---- POST /v1/users ----

    @Test
    void createUser_정상_201_래퍼shape() throws Exception {
        mockMvc.perform(post("/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateUserRequest("게이머"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.nickname").value("게이머"))
                .andExpect(jsonPath("$.data.createdAt").exists());
    }

    @Test
    void createUser_빈닉네임_400() throws Exception {
        mockMvc.perform(post("/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createUser_닉네임길이초과_400() throws Exception {
        String tooLong = "13자를넘는닉네임입니다요"; // 13자 (max=12 초과)
        mockMvc.perform(post("/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateUserRequest(tooLong))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createUser_nickname필드누락_400() throws Exception {
        mockMvc.perform(post("/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ---- GET /v1/users/{userId} ----

    @Test
    void getUser_정상_200_래퍼shape() throws Exception {
        User saved = userRepository.save(new User("홍길동"));
        String id = saved.getId().toHexString();

        mockMvc.perform(get("/v1/users/{userId}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.nickname").value("홍길동"))
                .andExpect(jsonPath("$.data.createdAt").exists());
    }

    @Test
    void getUser_존재하지않으면_404_에러래퍼() throws Exception {
        String id = new ObjectId().toHexString();

        mockMvc.perform(get("/v1/users/{userId}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void getUser_잘못된ObjectId형식_400() throws Exception {
        mockMvc.perform(get("/v1/users/{userId}", "not-a-valid-objectid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
