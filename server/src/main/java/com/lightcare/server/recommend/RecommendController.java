package com.lightcare.server.recommend;

import com.lightcare.server.common.ApiResponse;
import com.lightcare.server.common.CurrentUserAnnotation;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/v1/recommend")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendEngine engine;
    private final RecommendCardRepository cardRepo;

    public record CardDto(long id, String kind, String status, String title, String body) {}
    public record ExerciseReq(long profileId, int fatigue, int stepsToday) {}

    @GetMapping("/today")
    public ApiResponse<List<CardDto>> today(@CurrentUserAnnotation long userId, @RequestParam long profileId) {
        // 简化：每次都生成新的（真实场景需查"今日是否已推过"）
        var mealRecs = engine.recommendMeal(profileId);
        var cards = new java.util.ArrayList<RecommendCardEntity>();
        mealRecs.forEach(r -> {
            RecommendCardEntity c = new RecommendCardEntity();
            c.setProfileId(profileId);
            c.setKind(RecommendCardEntity.Kind.MEAL);
            c.setStatus(RecommendCardEntity.Status.PENDING);
            c.setTitle(r.title());
            c.setBody(r.body());
            cards.add(c);
        });
        cardRepo.saveAll(cards);
        return ApiResponse.ok(cards.stream().map(this::toDto).toList());
    }

    @PostMapping("/exercise")
    @Transactional
    public ApiResponse<List<CardDto>> exercise(@CurrentUserAnnotation long userId, @RequestBody ExerciseReq req) {
        var recs = engine.recommendExercise(req.profileId(), req.fatigue(), req.stepsToday());
        var cards = new java.util.ArrayList<RecommendCardEntity>();
        recs.forEach(r -> {
            RecommendCardEntity c = new RecommendCardEntity();
            c.setProfileId(req.profileId());
            c.setKind(RecommendCardEntity.Kind.EXERCISE);
            c.setStatus(RecommendCardEntity.Status.PENDING);
            c.setTitle(r.title());
            c.setBody(r.body());
            cards.add(c);
        });
        cardRepo.saveAll(cards);
        return ApiResponse.ok(cards.stream().map(this::toDto).toList());
    }

    @PostMapping("/{id}/accept")
    public ApiResponse<Void> accept(@PathVariable long id) {
        return updateStatus(id, RecommendCardEntity.Status.ACCEPTED);
    }

    @PostMapping("/{id}/skip")
    public ApiResponse<Void> skip(@PathVariable long id) {
        return updateStatus(id, RecommendCardEntity.Status.SKIPPED);
    }

    private ApiResponse<Void> updateStatus(long id, RecommendCardEntity.Status status) {
        cardRepo.findById(id).ifPresent(c -> {
            c.setStatus(status);
            c.setActedAt(Instant.now());
            cardRepo.save(c);
        });
        return ApiResponse.ok(null);
    }

    private CardDto toDto(RecommendCardEntity c) {
        return new CardDto(c.getId(), c.getKind().name(), c.getStatus().name(), c.getTitle(), c.getBody());
    }
}
