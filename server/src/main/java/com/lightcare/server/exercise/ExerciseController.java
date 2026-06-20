package com.lightcare.server.exercise;

import com.lightcare.server.common.ApiError;
import com.lightcare.server.common.ApiException;
import com.lightcare.server.common.ApiResponse;
import com.lightcare.server.common.CurrentUserAnnotation;
import com.lightcare.server.profile.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/v1/exercises")
@RequiredArgsConstructor
public class ExerciseController {

    private final ExerciseRepository exerciseRepo;
    private final WaterLogRepository waterRepo;
    private final ProfileRepository profileRepo;

    public record ExerciseDto(
        long id, long profileId, String kind, String intensity,
        int durationMin, int steps, int fatigue, LocalDate exerciseDate
    ) {}

    public record CreateExerciseReq(
        long profileId, String kind, String intensity,
        int durationMin, int steps, int fatigue
    ) {}

    public record AddWaterReq(long profileId, int cups) {}

    public record TodaySummaryRes(
        long profileId, LocalDate date,
        int kcal, double proteinG, int vegServings, int waterCups,
        int exerciseMin
    ) {}

    @PostMapping
    @Transactional
    public ApiResponse<ExerciseDto> create(@CurrentUserAnnotation long userId, @RequestBody CreateExerciseReq req) {
        mustOwn(userId, req.profileId());
        ExerciseEntity e = new ExerciseEntity();
        e.setProfileId(req.profileId());
        e.setKind(parseKind(req.kind()));
        e.setIntensity(parseIntensity(req.intensity()));
        e.setDurationMin(req.durationMin());
        e.setSteps(req.steps());
        e.setFatigue(req.fatigue());
        e.setExerciseDate(LocalDate.now());
        exerciseRepo.save(e);
        return ApiResponse.ok(toDto(e));
    }

    @PostMapping("/water")
    public ApiResponse<Void> addWater(@CurrentUserAnnotation long userId, @RequestBody AddWaterReq req) {
        mustOwn(userId, req.profileId());
        WaterLogEntity w = new WaterLogEntity();
        w.setProfileId(req.profileId());
        w.setCups(Math.max(1, req.cups()));
        w.setLogDate(LocalDate.now());
        waterRepo.save(w);
        return ApiResponse.ok(null);
    }

    @GetMapping("/today")
    public ApiResponse<TodaySummaryRes> today(@CurrentUserAnnotation long userId, @RequestParam long profileId) {
        mustOwn(userId, profileId);
        LocalDate d = LocalDate.now();
        return ApiResponse.ok(new TodaySummaryRes(
            profileId, d, 0, 0, 0, waterRepo.sumCupsByDate(profileId, d),
            exerciseRepo.sumDurationByDate(profileId, d)
        ));
    }

    private void mustOwn(long userId, long profileId) {
        var p = profileRepo.findById(profileId).orElseThrow(() -> new ApiException(ApiError.NOT_FOUND));
        if (p.getOwnerUserId() != userId && !java.util.Objects.equals(p.getManagedByUserId(), userId)) {
            throw new ApiException(ApiError.FORBIDDEN);
        }
    }

    private ExerciseEntity.Kind parseKind(String s) {
        if (s == null) return ExerciseEntity.Kind.WALK;
        try { return ExerciseEntity.Kind.valueOf(s.toUpperCase()); }
        catch (Exception e) { return ExerciseEntity.Kind.OTHER; }
    }
    private ExerciseEntity.Intensity parseIntensity(String s) {
        if (s == null) return ExerciseEntity.Intensity.LIGHT;
        try { return ExerciseEntity.Intensity.valueOf(s.toUpperCase()); }
        catch (Exception e) { return ExerciseEntity.Intensity.LIGHT; }
    }
    private ExerciseDto toDto(ExerciseEntity e) {
        return new ExerciseDto(
            e.getId(), e.getProfileId(), e.getKind().name(), e.getIntensity().name(),
            e.getDurationMin(), e.getSteps(), e.getFatigue(), e.getExerciseDate()
        );
    }
}
