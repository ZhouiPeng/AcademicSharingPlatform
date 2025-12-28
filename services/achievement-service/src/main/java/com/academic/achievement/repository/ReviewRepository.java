package com.academic.achievement.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.academic.achievement.entity.ReviewEntity;

public interface ReviewRepository extends JpaRepository<ReviewEntity, String> {

    default void insertReviewEntity(String achId, String userId) {
        ReviewEntity e = new ReviewEntity();
        e.setAchId(achId);
        e.setUserId(userId);
        save(e);
    }

    /**
     * Safe helper to check whether a review record exists for the given
     * achievement id. Delegates to {@code existsById} from JpaRepository and
     * returns false on invalid input or errors.
     */
    default boolean existsByAchId(String achId) {
        if (achId == null || achId.isBlank()) {
            return false;
        }
        try {
            return existsById(achId);
        } catch (Exception ex) {
            return false;
        }
    }
}
