package com.lightcare.server.recommend;

import com.lightcare.server.common.ApiError;
import com.lightcare.server.common.ApiException;
import com.lightcare.server.common.ApiResponse;
import com.lightcare.server.common.CurrentUserAnnotation;
import com.lightcare.server.profile.ProfileRepository;
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
    private final ProfileRepository profileRepo;   // 鉴权薄弱点补：用 profile 归属校验

    public record CardDto(long id, String kind, String status, String title, String body, Long foodId) {}
    public record ExerciseReq(long profileId, int fatigue, int stepsToday) {}

    @GetMapping("/today")
    public ApiResponse<List<CardDto>> today(@CurrentUserAnnotation long userId, @RequestParam long profileId) {
        mustOwn(userId, profileId);
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
            c.setFoodId(r.foodId());    // PR-Recipe: meal → 关联食物
            cards.add(c);
        });
        cardRepo.saveAll(cards);
        return ApiResponse.ok(cards.stream().map(this::toDto).toList());
    }

    @PostMapping("/exercise")
    @Transactional
    public ApiResponse<List<CardDto>> exercise(@CurrentUserAnnotation long userId, @RequestBody ExerciseReq req) {
        mustOwn(userId, req.profileId());
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
    public ApiResponse<Void> accept(@CurrentUserAnnotation long userId, @PathVariable long id) {
        return updateStatus(userId, id, RecommendCardEntity.Status.ACCEPTED);
    }

    @PostMapping("/{id}/skip")
    public ApiResponse<Void> skip(@CurrentUserAnnotation long userId, @PathVariable long id) {
        return updateStatus(userId, id, RecommendCardEntity.Status.SKIPPED);
    }

    private ApiResponse<Void> updateStatus(long userId, long id, RecommendCardEntity.Status status) {
        RecommendCardEntity c = cardRepo.findById(id)
            .orElseThrow(() -> new ApiException(ApiError.NOT_FOUND));
        // 鉴权薄弱点补：必须 userId 拥有该 card 对应的 profile（owner 或 manager）
        mustOwn(userId, c.getProfileId());
        c.setStatus(status);
        c.setActedAt(Instant.now());
        cardRepo.save(c);
        return ApiResponse.ok(null);
    }

    /** 鉴权薄弱点补：要求 userId 是 profile 的 owner 或 manager。 */
    private void mustOwn(long userId, long profileId) {
        var p = profileRepo.findById(profileId)
            .orElseThrow(() -> new ApiException(ApiError.NOT_FOUND));
        if (p.getOwnerUserId() != userId && !java.util.Objects.equals(p.getManagedByUserId(), userId)) {
            throw new ApiException(ApiError.FORBIDDEN);
        }
    }

    private CardDto toDto(RecommendCardEntity c) {
        return new CardDto(c.getId(), c.getKind().name(), c.getStatus().name(),
            c.getTitle(), c.getBody(), c.getFoodId());
    }
}
