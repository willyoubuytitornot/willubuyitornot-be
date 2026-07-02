package com.willu.buyitornot.infra.repository;

import com.willu.buyitornot.infra.collection.Swipe;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SwipeRepository extends MongoRepository<Swipe, ObjectId> {

    Optional<Swipe> findFirstByOrderByCreatedAtDesc();
}
