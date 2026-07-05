package com.willu.buyitornot.infra.repository;

import com.willu.buyitornot.infra.collection.UserSwipe;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSwipeRepository extends MongoRepository<UserSwipe, ObjectId> {

    Optional<UserSwipe> findByUserIdAndSwipeId(ObjectId userId, ObjectId swipeId);
}
