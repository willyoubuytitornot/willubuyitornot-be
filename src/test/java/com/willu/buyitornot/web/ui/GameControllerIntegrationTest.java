package com.willu.buyitornot.web.ui;

import com.willu.buyitornot.infra.collection.Game;
import com.willu.buyitornot.infra.repository.GameRepository;
import com.willu.buyitornot.support.AbstractMongoIntegrationTest;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GameController(web.ui) + GameService(service) — Testcontainers MongoDB에 연결된 전체 컨텍스트로
 * 실제 서비스/리포지토리 계층까지 거쳐 검증한다(모킹하지 않음).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class GameControllerIntegrationTest extends AbstractMongoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GameRepository gameRepository;

    @AfterEach
    void cleanUp() {
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

    // ---- GET /v1/games/{gameId} ----

    @Test
    void get_정상조회_200_래퍼shape_모든필드매핑() throws Exception {
        Game saved = saveGame("game1");
        String id = saved.getId().toHexString();

        mockMvc.perform(get("/v1/games/{gameId}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.title").value("game1"))
                .andExpect(jsonPath("$.data.genre").value("RPG"))
                .andExpect(jsonPath("$.data.imageUrl").value("https://example.com/game1.jpg"))
                .andExpect(jsonPath("$.data.releaseDate").value("2024-03-15"))
                .andExpect(jsonPath("$.data.aiComment").value("한줄평: game1"))
                .andExpect(jsonPath("$.data.storeUrl").value("https://store.example.com/game1"))
                .andExpect(jsonPath("$.data.communityUrl").value("https://community.example.com/game1"))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.updatedAt").exists());
    }

    @Test
    void get_존재하지않는gameId_404_에러래퍼() throws Exception {
        String randomId = new ObjectId().toHexString();

        mockMvc.perform(get("/v1/games/{gameId}", randomId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("Game")))
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString(randomId)));
    }

    @Test
    void get_잘못된ObjectId형식_400() throws Exception {
        mockMvc.perform(get("/v1/games/{gameId}", "not-an-objectid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
