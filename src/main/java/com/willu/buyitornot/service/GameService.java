package com.willu.buyitornot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.willu.buyitornot.exception.ResourceNotFoundException;
import com.willu.buyitornot.infra.collection.Game;
import com.willu.buyitornot.infra.repository.GameRepository;
import com.willu.buyitornot.web.dto.response.GameInsightResponse;
import com.willu.buyitornot.web.dto.response.GameResponse;
import com.willu.buyitornot.web.dto.response.SentimentDto;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;
    private final GeminiService geminiService;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public GameResponse getGame(String gameId) {
        ObjectId objectId = new ObjectId(gameId);
        Game game = gameRepository.findById(objectId)
                .orElseThrow(() -> new ResourceNotFoundException("Game", "id", gameId));
        return toResponse(game);
    }

    public GameInsightResponse getInsight(String gameId) {
        ObjectId objectId = new ObjectId(gameId);
        Game game = gameRepository.findById(objectId)
                .orElseThrow(() -> new ResourceNotFoundException("Game", "id", gameId));

        String prompt = """
                당신은 게임 전문 리뷰어입니다. 아래 게임에 대한 커뮤니티 반응을 분석해 JSON으로만 응답하세요.

                [게임 정보]
                - 제목: %s
                - 장르: %s
                - 한줄 소개: %s

                [분석 규칙]
                1. 해당 게임의 실제 평판과 장르 특성을 반영해 현실적인 수치를 생성한다.
                2. summary는 50자 이내 한국어로 커뮤니티 반응을 요약한다.
                3. sentiments는 장르에 맞는 4개 항목(예: 스토리/그래픽/조작감/가성비)으로 구성한다.
                4. pros는 2~3개, cons는 1~2개 한국어로 작성한다.

                [응답 형식 - JSON만, 그 외 텍스트 없이]
                {
                  "rating": 4.2,
                  "reviews": 1500,
                  "positive": 78,
                  "summary": "50자 이내 커뮤니티 반응 요약",
                  "sentiments": [
                    {"label": "항목명", "score": 85}
                  ],
                  "pros": ["장점1", "장점2"],
                  "cons": ["단점1"]
                }
                """.formatted(game.getTitle(), game.getGenre(), game.getAiComment());

        String raw = geminiService.chat(prompt);
        return parseInsight(gameId, raw);
    }

    private GameInsightResponse parseInsight(String gameId, String raw) {
        try {
            String json = raw.trim();
            int start = json.indexOf("{");
            int end = json.lastIndexOf("}");
            if (start >= 0 && end > start) {
                json = json.substring(start, end + 1);
            }
            JsonNode node = OBJECT_MAPPER.readTree(json);

            float rating = (float) node.path("rating").asDouble(4.0);
            int reviews = node.path("reviews").asInt(0);
            int positive = node.path("positive").asInt(50);
            String summary = node.path("summary").asText("");

            List<SentimentDto> sentiments = new ArrayList<>();
            node.path("sentiments").forEach(s ->
                    sentiments.add(new SentimentDto(s.path("label").asText(), s.path("score").asInt()))
            );

            List<String> pros = new ArrayList<>();
            node.path("pros").forEach(p -> pros.add(p.asText()));

            List<String> cons = new ArrayList<>();
            node.path("cons").forEach(c -> cons.add(c.asText()));

            return new GameInsightResponse(gameId, rating, reviews, positive, 100 - positive, summary, sentiments, pros, cons);
        } catch (Exception e) {
            return new GameInsightResponse(gameId, 0f, 0, 0, 0, raw, List.of(), List.of(), List.of());
        }
    }

    private GameResponse toResponse(Game game) {
        return new GameResponse(
                game.getId().toHexString(),
                game.getImageUrl(),
                game.getGenre(),
                game.getReleaseDate(),
                game.getTitle(),
                game.getAiComment(),
                game.getStoreUrl(),
                game.getCommunityUrl(),
                game.getCreatedAt(),
                game.getUpdatedAt()
        );
    }
}
