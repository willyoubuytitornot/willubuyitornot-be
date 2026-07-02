package com.willu.buyitornot.web.ui;

import com.willu.buyitornot.infra.collection.Game;
import com.willu.buyitornot.infra.collection.Swipe;
import com.willu.buyitornot.infra.repository.GameRepository;
import com.willu.buyitornot.infra.repository.SwipeRepository;
import com.willu.buyitornot.support.AbstractMongoIntegrationTest;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SwipeController(web.ui) + SwipeService(service) — Testcontainers MongoDB에 연결된 전체 컨텍스트로
 * 실제 서비스/리포지토리 계층(gameIdList → Game 확장 join 포함)까지 거쳐 검증한다(모킹하지 않음).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class SwipeControllerIntegrationTest extends AbstractMongoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SwipeRepository swipeRepository;

    @Autowired
    private GameRepository gameRepository;

    @AfterEach
    void cleanUp() {
        swipeRepository.deleteAll();
        gameRepository.deleteAll();
    }

    private Game saveGame(String title) {
        return gameRepository.save(new Game(
                "https://example.com/" + title + ".jpg",
                "RPG",
                LocalDate.of(2024, 3, 15),
                title,
                "한줄평: " + title,
                "https://store.example.com/" + title,
                "https://community.example.com/" + title
        ));
    }

    // ---- GET /v1/swipes/current ----

    @Test
    void current_정상조회_최신라운드_games확장_래퍼shape() throws Exception {
        Game g1 = saveGame("game1");
        Game g2 = saveGame("game2");

        // 더 오래된 swipe 먼저 저장
        swipeRepository.save(new Swipe(List.of(g1.getId())));
        // 최신 swipe (조회 대상)
        Swipe latest = swipeRepository.save(new Swipe(List.of(g2.getId(), g1.getId())));

        mockMvc.perform(get("/v1/swipes/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(latest.getId().toHexString()))
                .andExpect(jsonPath("$.data.gameIdList[0]").value(g2.getId().toHexString()))
                .andExpect(jsonPath("$.data.gameIdList[1]").value(g1.getId().toHexString()))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.games.length()").value(2))
                // gameIdList 순서(g2, g1)와 games 순서가 일치해야 한다 (findAllById는 순서 미보장이므로 회귀 포인트)
                .andExpect(jsonPath("$.data.games[0].id").value(g2.getId().toHexString()))
                .andExpect(jsonPath("$.data.games[0].title").value("game2"))
                .andExpect(jsonPath("$.data.games[1].id").value(g1.getId().toHexString()))
                .andExpect(jsonPath("$.data.games[1].title").value("game1"));
    }

    @Test
    void current_라운드없음_404_에러래퍼() throws Exception {
        mockMvc.perform(get("/v1/swipes/current"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("Swipe")))
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("current")));
    }

    // ---- GET /v1/swipes/{swipeId} ----

    @Test
    void getById_정상조회_200_래퍼shape() throws Exception {
        Game g1 = saveGame("game1");
        Swipe swipe = swipeRepository.save(new Swipe(List.of(g1.getId())));

        mockMvc.perform(get("/v1/swipes/{swipeId}", swipe.getId().toHexString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(swipe.getId().toHexString()))
                .andExpect(jsonPath("$.data.games[0].id").value(g1.getId().toHexString()))
                .andExpect(jsonPath("$.data.games[0].title").value("game1"));
    }

    @Test
    void getById_존재하지않는swipeId_404() throws Exception {
        String randomId = new ObjectId().toHexString();

        mockMvc.perform(get("/v1/swipes/{swipeId}", randomId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void getById_잘못된형식_400() throws Exception {
        mockMvc.perform(get("/v1/swipes/{swipeId}", "invalid-id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getById_gameIdList중일부게임없음_존재하는게임만원본순서로반환() throws Exception {
        Game g1 = saveGame("game1");
        ObjectId missingId = new ObjectId(); // games 컬렉션에 저장하지 않음
        Game g2 = saveGame("game2");

        Swipe swipe = swipeRepository.save(new Swipe(List.of(g1.getId(), missingId, g2.getId())));

        mockMvc.perform(get("/v1/swipes/{swipeId}", swipe.getId().toHexString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                // gameIdList 자체는 필터링 없이 원본 그대로 노출(누락 id 포함 3개)
                .andExpect(jsonPath("$.data.gameIdList.length()").value(3))
                .andExpect(jsonPath("$.data.gameIdList[1]").value(missingId.toHexString()))
                // games는 존재하는 2개만, 원본 순서(g1, g2) 유지
                .andExpect(jsonPath("$.data.games.length()").value(2))
                .andExpect(jsonPath("$.data.games[0].id").value(g1.getId().toHexString()))
                .andExpect(jsonPath("$.data.games[1].id").value(g2.getId().toHexString()));
    }

    @Test
    void getById_gameIdList가빈리스트_200_games빈배열() throws Exception {
        Swipe swipe = swipeRepository.save(new Swipe(List.of()));

        mockMvc.perform(get("/v1/swipes/{swipeId}", swipe.getId().toHexString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.gameIdList.length()").value(0))
                .andExpect(jsonPath("$.data.games.length()").value(0));
    }
}
