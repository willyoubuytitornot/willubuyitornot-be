package com.willu.buyitornot.service;

import com.willu.buyitornot.exception.ResourceNotFoundException;
import com.willu.buyitornot.infra.collection.Game;
import com.willu.buyitornot.infra.repository.GameRepository;
import com.willu.buyitornot.web.dto.response.GameInsightResponse;
import com.willu.buyitornot.web.dto.response.GameResponse;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;

    public GameResponse getGame(String gameId) {
        ObjectId objectId = new ObjectId(gameId);
        Game game = gameRepository.findById(objectId)
                .orElseThrow(() -> new ResourceNotFoundException("Game", "id", gameId));
        return toResponse(game);
    }

    public GameInsightResponse getInsight(String gameId) {
        throw new UnsupportedOperationException("Not implemented");
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
