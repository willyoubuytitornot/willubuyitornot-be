package com.willu.buyitornot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.willu.buyitornot.exception.ResourceNotFoundException;
import com.willu.buyitornot.infra.collection.Game;
import com.willu.buyitornot.infra.collection.UserSwipe;
import com.willu.buyitornot.infra.repository.GameRepository;
import com.willu.buyitornot.infra.repository.SwipeRepository;
import com.willu.buyitornot.infra.repository.UserRepository;
import com.willu.buyitornot.infra.repository.UserSwipeRepository;
import com.willu.buyitornot.web.dto.request.VoteRequest;
import com.willu.buyitornot.web.dto.response.GenreStatDto;
import com.willu.buyitornot.web.dto.response.PersonaDto;
import com.willu.buyitornot.web.dto.response.PreferenceReportResponse;
import com.willu.buyitornot.web.dto.response.UserSwipeResponse;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserSwipeService {

    private final UserSwipeRepository userSwipeRepository;
    private final UserRepository userRepository;
    private final SwipeRepository swipeRepository;
    private final GameRepository gameRepository;
    private final GeminiService geminiService;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public UserSwipeResponse getResult(String userId, String swipeId) {
        ObjectId userOid = new ObjectId(userId);
        ObjectId swipeOid = new ObjectId(swipeId);
        return userSwipeRepository.findByUserIdAndSwipeId(userOid, swipeOid)
                .map(this::toResponse)
                .orElse(new UserSwipeResponse(null, userId, swipeId, List.of(), List.of(), List.of(), null, null));
    }

    public UserSwipeResponse vote(String userId, String swipeId, List<VoteRequest> requests) {
        ObjectId userOid = new ObjectId(userId);
        ObjectId swipeOid = new ObjectId(swipeId);

        if (!userRepository.existsById(userOid)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }
        if (!swipeRepository.existsById(swipeOid)) {
            throw new ResourceNotFoundException("Swipe", "id", swipeId);
        }

        UserSwipe userSwipe = userSwipeRepository.findByUserIdAndSwipeId(userOid, swipeOid)
                .orElse(new UserSwipe(userOid, swipeOid));

        List<ObjectId> buy = new ArrayList<>();
        List<ObjectId> skip = new ArrayList<>();
        List<ObjectId> maybe = new ArrayList<>();

        for (VoteRequest req : requests) {
            ObjectId gameOid = new ObjectId(req.gameId());
            switch (req.decision()) {
                case BUY -> buy.add(gameOid);
                case SKIP -> skip.add(gameOid);
                case MAYBE -> maybe.add(gameOid);
            }
        }

        userSwipe.setBuy(buy);
        userSwipe.setSkip(skip);
        userSwipe.setMaybe(maybe);
        userSwipe.setUpdatedAt(LocalDateTime.now());

        return toResponse(userSwipeRepository.save(userSwipe));
    }

    public PreferenceReportResponse getReport(String userId, String swipeId) {
        ObjectId userOid = new ObjectId(userId);
        ObjectId swipeOid = new ObjectId(swipeId);

        UserSwipe userSwipe = userSwipeRepository.findByUserIdAndSwipeId(userOid, swipeOid)
                .orElseThrow(() -> new ResourceNotFoundException("UserSwipe", "userId+swipeId", userId + "+" + swipeId));

        List<ObjectId> buyIds = userSwipe.getBuy() != null ? userSwipe.getBuy() : List.of();
        List<ObjectId> skipIds = userSwipe.getSkip() != null ? userSwipe.getSkip() : List.of();
        List<ObjectId> maybeIds = userSwipe.getMaybe() != null ? userSwipe.getMaybe() : List.of();

        int buyCount = buyIds.size();
        int skipCount = skipIds.size();
        int maybeCount = maybeIds.size();
        int totalCount = buyCount + skipCount + maybeCount;

        List<Game> boughtGames = buyIds.isEmpty() ? List.of() : gameRepository.findAllById(buyIds);

        Map<String, Long> genreCountMap = boughtGames.stream()
                .filter(g -> g.getGenre() != null)
                .collect(Collectors.groupingBy(Game::getGenre, Collectors.counting()));

        long totalBought = boughtGames.size();
        List<GenreStatDto> genreStats = genreCountMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> new GenreStatDto(
                        e.getKey(),
                        e.getValue().intValue(),
                        totalBought > 0 ? (int) Math.round(e.getValue() * 100.0 / totalBought) : 0
                ))
                .toList();

        PersonaDto persona = generatePersona(genreStats, buyCount, skipCount, maybeCount, totalCount);

        return new PreferenceReportResponse(totalCount, buyCount, skipCount, maybeCount, genreStats, persona);
    }

    private PersonaDto generatePersona(List<GenreStatDto> genreStats, int buyCount, int skipCount, int maybeCount, int totalCount) {
        String genreDesc = genreStats.isEmpty() ? "장르 정보 없음" :
                genreStats.stream()
                        .map(g -> g.genre() + " " + g.percentage() + "%")
                        .collect(Collectors.joining(", "));

        String prompt = """
                당신은 게임 취향 분석 전문가입니다. 아래 스와이프 투표 데이터를 바탕으로 JSON으로만 응답하세요.

                [투표 데이터]
                - buy(구매 의향): %d개, skip(패스): %d개, maybe(고민중): %d개
                - buy한 게임의 장르 비율: %s

                [분석 규칙 - 반드시 준수]
                1. 데이터 개수, 데이터 양, 표본 수에 대해 절대 언급하지 않는다.
                2. buy 장르 비율을 근거로 취향을 단정적으로 묘사한다.
                3. buy/skip/maybe 비율로 구매 성향(충동형 vs 신중형 vs 탐색형)을 반영한다.
                4. description은 100자 이내, 한국어, 친근한 말투로 작성한다.

                [응답 형식 - JSON만, 그 외 텍스트 없이]
                {
                  "title": "5자 이내 페르소나 타이틀",
                  "description": "100자 이내 취향 설명",
                  "tags": ["#태그1", "#태그2", "#태그3"]
                }
                """.formatted(buyCount, skipCount, maybeCount, genreDesc);

        return parsePersona(geminiService.chat(prompt));
    }

    private PersonaDto parsePersona(String raw) {
        try {
            String json = raw.trim();
            int start = json.indexOf("{");
            int end = json.lastIndexOf("}");
            if (start >= 0 && end > start) {
                json = json.substring(start, end + 1);
            }
            JsonNode node = OBJECT_MAPPER.readTree(json);
            String title = node.path("title").asText("게임 탐험가");
            String description = node.path("description").asText(raw);
            List<String> tags = new ArrayList<>();
            node.path("tags").forEach(t -> tags.add(t.asText()));
            return new PersonaDto(title, description, tags);
        } catch (Exception e) {
            return new PersonaDto("게임 탐험가", raw, List.of());
        }
    }

    private UserSwipeResponse toResponse(UserSwipe us) {
        return new UserSwipeResponse(
                us.getId().toHexString(),
                us.getUserId().toHexString(),
                us.getSwipeId().toHexString(),
                toHexList(us.getBuy()),
                toHexList(us.getSkip()),
                toHexList(us.getMaybe()),
                us.getCreatedAt(),
                us.getUpdatedAt()
        );
    }

    private List<String> toHexList(List<ObjectId> ids) {
        if (ids == null) return List.of();
        return ids.stream().map(ObjectId::toHexString).toList();
    }
}
