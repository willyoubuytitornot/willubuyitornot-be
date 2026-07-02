package com.willu.buyitornot.service;

import com.willu.buyitornot.exception.ResourceNotFoundException;
import com.willu.buyitornot.infra.collection.Game;
import com.willu.buyitornot.infra.collection.Swipe;
import com.willu.buyitornot.infra.repository.GameRepository;
import com.willu.buyitornot.infra.repository.SwipeRepository;
import com.willu.buyitornot.web.dto.response.GameResponse;
import com.willu.buyitornot.web.dto.response.SwipeWithGamesResponse;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SwipeService {

    private final SwipeRepository swipeRepository;
    private final GameRepository gameRepository;

    public SwipeWithGamesResponse getCurrent() {
        Swipe swipe = swipeRepository.findFirstByOrderByCreatedAtDesc()
                .orElseThrow(() -> new ResourceNotFoundException("Swipe", "current", "latest"));
        return toResponse(swipe);
    }

    public SwipeWithGamesResponse getById(String swipeId) {
        ObjectId objectId = new ObjectId(swipeId);
        Swipe swipe = swipeRepository.findById(objectId)
                .orElseThrow(() -> new ResourceNotFoundException("Swipe", "id", swipeId));
        return toResponse(swipe);
    }

    private SwipeWithGamesResponse toResponse(Swipe swipe) {
        List<ObjectId> gameIdList = swipe.getGameIdList() != null ? swipe.getGameIdList() : List.of();

        Map<ObjectId, Game> gameById = gameRepository.findAllById(gameIdList).stream()
                .collect(Collectors.toMap(Game::getId, game -> game));

        List<GameResponse> games = gameIdList.stream()
                .map(gameById::get)
                .filter(Objects::nonNull)
                .map(this::toGameResponse)
                .toList();

        List<String> gameIdHexList = gameIdList.stream()
                .map(ObjectId::toHexString)
                .toList();

        return new SwipeWithGamesResponse(
                swipe.getId().toHexString(),
                gameIdHexList,
                swipe.getCreatedAt(),
                games
        );
    }

    private GameResponse toGameResponse(Game game) {
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
