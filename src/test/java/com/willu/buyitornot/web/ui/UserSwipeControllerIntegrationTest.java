package com.willu.buyitornot.web.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.willu.buyitornot.infra.collection.Game;
import com.willu.buyitornot.infra.collection.Swipe;
import com.willu.buyitornot.infra.collection.User;
import com.willu.buyitornot.infra.repository.GameRepository;
import com.willu.buyitornot.infra.repository.SwipeRepository;
import com.willu.buyitornot.infra.repository.UserRepository;
import com.willu.buyitornot.infra.repository.UserSwipeRepository;
import com.willu.buyitornot.service.GeminiService;
import com.willu.buyitornot.support.AbstractMongoIntegrationTest;
import com.willu.buyitornot.web.dto.Decision;
import com.willu.buyitornot.web.dto.request.VoteRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class UserSwipeControllerIntegrationTest extends AbstractMongoIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired SwipeRepository swipeRepository;
    @Autowired GameRepository gameRepository;
    @Autowired UserSwipeRepository userSwipeRepository;

    @MockBean
    GeminiService geminiService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private User user;
    private Swipe swipe;
    private Game game1;
    private Game game2;

    @BeforeEach
    void setUp() {
        user = userRepository.save(new User("테스터"));
        game1 = gameRepository.save(new Game("img1", "RPG", LocalDate.now(), "게임1", "코멘트", "store1", "community1"));
        game2 = gameRepository.save(new Game("img2", "액션", LocalDate.now(), "게임2", "코멘트", "store2", "community2"));
        swipe = swipeRepository.save(new Swipe(List.of(game1.getId(), game2.getId())));

        when(geminiService.chat(anyString())).thenReturn("""
                {"title":"RPG덕후","description":"깊은 세계관과 성장 스토리를 즐기는 진정한 RPG 마니아입니다.","tags":["#RPG","#세계관","#성장"]}
                """);
    }

    @AfterEach
    void tearDown() {
        userSwipeRepository.deleteAll();
        gameRepository.deleteAll();
        swipeRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ---- GET /v1/users/{userId}/swipes/{swipeId} (미참여) ----

    @Test
    void getResult_미참여_빈배열_200() throws Exception {
        mockMvc.perform(get("/v1/users/{userId}/swipes/{swipeId}", user.getId().toHexString(), swipe.getId().toHexString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.buy").isArray())
                .andExpect(jsonPath("$.data.skip").isArray())
                .andExpect(jsonPath("$.data.maybe").isArray())
                .andExpect(jsonPath("$.data.buy.length()").value(0))
                .andExpect(jsonPath("$.data.skip.length()").value(0))
                .andExpect(jsonPath("$.data.maybe.length()").value(0));
    }

    // ---- POST /v1/users/{userId}/swipes/{swipeId}/votes ----

    @Test
    void vote_정상_200_래퍼shape() throws Exception {
        List<VoteRequest> requests = List.of(
                new VoteRequest(game1.getId().toHexString(), Decision.BUY),
                new VoteRequest(game2.getId().toHexString(), Decision.SKIP)
        );

        mockMvc.perform(post("/v1/users/{userId}/swipes/{swipeId}/votes",
                        user.getId().toHexString(), swipe.getId().toHexString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.userId").value(user.getId().toHexString()))
                .andExpect(jsonPath("$.data.swipeId").value(swipe.getId().toHexString()))
                .andExpect(jsonPath("$.data.buy.length()").value(1))
                .andExpect(jsonPath("$.data.skip.length()").value(1))
                .andExpect(jsonPath("$.data.maybe.length()").value(0));
    }

    @Test
    void vote_재투표_기존결과_덮어씀() throws Exception {
        // 1차 투표
        List<VoteRequest> first = List.of(
                new VoteRequest(game1.getId().toHexString(), Decision.BUY),
                new VoteRequest(game2.getId().toHexString(), Decision.BUY)
        );
        mockMvc.perform(post("/v1/users/{userId}/swipes/{swipeId}/votes",
                        user.getId().toHexString(), swipe.getId().toHexString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isOk());

        // 2차 투표 (덮어쓰기)
        List<VoteRequest> second = List.of(
                new VoteRequest(game1.getId().toHexString(), Decision.SKIP),
                new VoteRequest(game2.getId().toHexString(), Decision.MAYBE)
        );
        mockMvc.perform(post("/v1/users/{userId}/swipes/{swipeId}/votes",
                        user.getId().toHexString(), swipe.getId().toHexString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.buy.length()").value(0))
                .andExpect(jsonPath("$.data.skip.length()").value(1))
                .andExpect(jsonPath("$.data.maybe.length()").value(1));
    }

    @Test
    void vote_존재하지않는유저_404() throws Exception {
        String fakeUserId = new org.bson.types.ObjectId().toHexString();
        List<VoteRequest> requests = List.of(new VoteRequest(game1.getId().toHexString(), Decision.BUY));

        mockMvc.perform(post("/v1/users/{userId}/swipes/{swipeId}/votes",
                        fakeUserId, swipe.getId().toHexString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void vote_존재하지않는스와이프_404() throws Exception {
        String fakeSwipeId = new org.bson.types.ObjectId().toHexString();
        List<VoteRequest> requests = List.of(new VoteRequest(game1.getId().toHexString(), Decision.BUY));

        mockMvc.perform(post("/v1/users/{userId}/swipes/{swipeId}/votes",
                        user.getId().toHexString(), fakeSwipeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ---- GET /v1/users/{userId}/swipes/{swipeId} (투표 후) ----

    @Test
    void getResult_투표후_결과조회_200() throws Exception {
        // 먼저 투표
        List<VoteRequest> requests = List.of(
                new VoteRequest(game1.getId().toHexString(), Decision.BUY),
                new VoteRequest(game2.getId().toHexString(), Decision.MAYBE)
        );
        mockMvc.perform(post("/v1/users/{userId}/swipes/{swipeId}/votes",
                        user.getId().toHexString(), swipe.getId().toHexString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk());

        // 결과 조회
        mockMvc.perform(get("/v1/users/{userId}/swipes/{swipeId}",
                        user.getId().toHexString(), swipe.getId().toHexString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.buy.length()").value(1))
                .andExpect(jsonPath("$.data.skip.length()").value(0))
                .andExpect(jsonPath("$.data.maybe.length()").value(1));
    }

    // ---- GET /v1/users/{userId}/swipes/{swipeId}/report ----

    @Test
    void getReport_미참여_404() throws Exception {
        mockMvc.perform(get("/v1/users/{userId}/swipes/{swipeId}/report",
                        user.getId().toHexString(), swipe.getId().toHexString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getReport_정상_200_래퍼shape() throws Exception {
        // 먼저 투표
        List<VoteRequest> requests = List.of(
                new VoteRequest(game1.getId().toHexString(), Decision.BUY),
                new VoteRequest(game2.getId().toHexString(), Decision.SKIP)
        );
        mockMvc.perform(post("/v1/users/{userId}/swipes/{swipeId}/votes",
                        user.getId().toHexString(), swipe.getId().toHexString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk());

        // 리포트 조회
        mockMvc.perform(get("/v1/users/{userId}/swipes/{swipeId}/report",
                        user.getId().toHexString(), swipe.getId().toHexString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.buyCount").value(1))
                .andExpect(jsonPath("$.data.skipCount").value(1))
                .andExpect(jsonPath("$.data.maybeCount").value(0))
                .andExpect(jsonPath("$.data.genreStats").isArray())
                .andExpect(jsonPath("$.data.genreStats.length()").value(1))
                .andExpect(jsonPath("$.data.genreStats[0].genre").value("RPG"))
                .andExpect(jsonPath("$.data.persona.title").value("RPG덕후"))
                .andExpect(jsonPath("$.data.persona.description").exists())
                .andExpect(jsonPath("$.data.persona.tags").isArray());
    }

    @Test
    void getReport_전부buy_장르통계_200() throws Exception {
        // game1(RPG), game2(액션) 모두 buy
        List<VoteRequest> requests = List.of(
                new VoteRequest(game1.getId().toHexString(), Decision.BUY),
                new VoteRequest(game2.getId().toHexString(), Decision.BUY)
        );
        mockMvc.perform(post("/v1/users/{userId}/swipes/{swipeId}/votes",
                        user.getId().toHexString(), swipe.getId().toHexString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v1/users/{userId}/swipes/{swipeId}/report",
                        user.getId().toHexString(), swipe.getId().toHexString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.buyCount").value(2))
                .andExpect(jsonPath("$.data.genreStats.length()").value(2));
    }
}
