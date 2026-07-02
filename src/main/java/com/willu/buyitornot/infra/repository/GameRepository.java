package com.willu.buyitornot.infra.repository;

import com.willu.buyitornot.infra.collection.Game;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameRepository extends MongoRepository<Game, ObjectId> {
}
