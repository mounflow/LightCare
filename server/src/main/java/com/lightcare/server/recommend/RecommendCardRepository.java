package com.lightcare.server.recommend;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecommendCardRepository extends JpaRepository<RecommendCardEntity, Long> {
    List<RecommendCardEntity> findByProfileIdAndStatusOrderByCreatedAtDesc(Long profileId, RecommendCardEntity.Status status);
}
