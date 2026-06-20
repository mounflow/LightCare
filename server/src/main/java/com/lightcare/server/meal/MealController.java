package com.lightcare.server.meal;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lightcare.server.common.ApiError;
import com.lightcare.server.common.ApiException;
import com.lightcare.server.common.ApiResponse;
import com.lightcare.server.common.CurrentUserAnnotation;
import com.lightcare.server.profile.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/v1/meals")
@RequiredArgsConstructor
@Slf4j
public class MealController {

    private final MealRepository mealRepo;
    private final ProfileRepository profileRepo;
    private final AiEstimateService aiService;
    private final MealRecognitionExecutor recognitionExecutor;
    private final ObjectMapper json = new ObjectMapper();

    @Value("${lightcare.upload-dir:uploads}")
    private String uploadDir;

    public record MealDto(
        long id, long profileId, String slot, String portion, String source,
        String summary, int kcal, double proteinG, double fatG, double carbG,
        double fiberG, int vegServings, int waterMl, String recognitionStatus,
        String location, String description, String itemsJson,
        LocalDate mealDate, LocalTime mealTime
    ) {}

    public record CreateMealReq(
        long profileId, String slot, String portion, String source,
        String summary, int kcal, double proteinG, double fatG, double carbG,
        double fiberG, int vegServings,
        Integer waterMl,
        String location, String description,
        List<String> foodKeys,
        String itemsJson
    ) {}

    @GetMapping
    public ApiResponse<List<MealDto>> listByDate(@CurrentUserAnnotation long userId,
                                                 @RequestParam long profileId,
                                                 @RequestParam String date) {
        mustOwn(userId, profileId);
        LocalDate d = LocalDate.parse(date);
        return ApiResponse.ok(
            mealRepo.findByProfileIdAndMealDateOrderByMealTime(profileId, d)
                .stream().map(this::toDto).toList()
        );
    }

    @GetMapping("/range")
    public ApiResponse<List<MealDto>> listByRange(@CurrentUserAnnotation long userId,
                                                  @RequestParam long profileId,
                                                  @RequestParam String from,
                                                  @RequestParam String to) {
        mustOwn(userId, profileId);
        LocalDate fromD = LocalDate.parse(from);
        LocalDate toD = LocalDate.parse(to);
        if (toD.isBefore(fromD)) {
            throw new ApiException(ApiError.BAD_REQUEST, "to 必须 >= from");
        }
        if (java.time.temporal.ChronoUnit.DAYS.between(fromD, toD) > 92) {
            throw new ApiException(ApiError.BAD_REQUEST, "范围不能超过 92 天");
        }
        return ApiResponse.ok(
            mealRepo.findByProfileIdAndDateRange(profileId, fromD, toD)
                .stream().map(this::toDto).toList()
        );
    }

    /** PR3: 单条查询，给客户端轮询识别状态用。 */
    @GetMapping("/{id}")
    public ApiResponse<MealDto> getOne(@CurrentUserAnnotation long userId, @PathVariable long id) {
        MealEntity m = mealRepo.findById(id).orElseThrow(() -> new ApiException(ApiError.NOT_FOUND));
        mustOwn(userId, m.getProfileId());
        return ApiResponse.ok(toDto(m));
    }

    /**
     * 取某条 meal 的拍照图片（详情页/时间线缩略图用）。
     * 图片存 ${uploadDir}/meal_<id>.jpg，不存在返 404。
     * 权限：必须 owns 该 meal（mustOwn 校验）。
     */
    @GetMapping(value = "/{id}/image", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<Resource> getImage(@CurrentUserAnnotation long userId, @PathVariable long id) {
        MealEntity m = mealRepo.findById(id).orElseThrow(() -> new ApiException(ApiError.NOT_FOUND));
        mustOwn(userId, m.getProfileId());
        Path target = Paths.get(uploadDir).resolve("meal_" + id + ".jpg");
        if (!Files.exists(target)) {
            throw new ApiException(ApiError.NOT_FOUND, "图片不存在");
        }
        Resource res = new FileSystemResource(target);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(res);
    }

    @PostMapping
    @Transactional
    public ApiResponse<MealDto> create(@CurrentUserAnnotation long userId, @RequestBody CreateMealReq req) {
        mustOwn(userId, req.profileId());
        MealEntity m = new MealEntity();
        m.setProfileId(req.profileId());
        m.setSlot(parseSlot(req.slot()));
        m.setPortion(parsePortion(req.portion()));
        m.setSource(parseSource(req.source()));
        m.setSummary(req.summary() == null ? "" : req.summary());
        m.setLocation(req.location() == null ? "" : req.location().trim());
        m.setDescription(req.description() == null ? "" : req.description().trim());
        m.setItemsJson(req.itemsJson() == null ? "" : req.itemsJson());

        if ((req.kcal() == 0 && req.proteinG() == 0) && req.foodKeys() != null && !req.foodKeys().isEmpty()) {
            AiEstimateService.EstimateResult est = aiService.estimate(req.foodKeys(), req.portion());
            m.setKcal(est.kcal());
            m.setProteinG(est.proteinG());
            m.setFatG(est.fatG());
            m.setCarbG(est.carbG());
            m.setFiberG(est.fiberG());
            m.setVegServings(est.vegServings());
        } else {
            m.setKcal(req.kcal());
            m.setProteinG(req.proteinG());
            m.setFatG(req.fatG());
            m.setCarbG(req.carbG());
            m.setFiberG(req.fiberG());
            m.setVegServings(req.vegServings());
            m.setWaterMl(req.waterMl() == null ? 0 : req.waterMl());
        }
        LocalDate today = LocalDate.now();
        m.setMealDate(today);
        // 截断到秒：Hibernate 6.5 + PG TIME 列对带微秒/纳秒的 LocalTime 反序列化时有 bug
        // （会算成 1秒-纳秒 的负值，导致 GET /v1/meals 整个列表 500 NanoOfSecond 异常）。
        // 餐次时间到秒足够，去掉小数秒从源头规避。
        m.setMealTime(LocalTime.now().truncatedTo(ChronoUnit.SECONDS));
        mealRepo.save(m);
        return ApiResponse.ok(toDto(m));
    }

    /**
     * PR3: 拍照即入列。
     * multipart: profileId/slot/portion/summary (form fields) + previewItems (json string) + image (file)
     * 同步：保存 meal(status=PENDING, 营养 0) + 持久化图片
     * 异步：调 MealRecognitionExecutor.enqueue → M3 + 写回
     */
    @PostMapping(value = "/photo", consumes = "multipart/form-data")
    @Transactional
    public ApiResponse<MealDto> createFromPhoto(
        @CurrentUserAnnotation long userId,
        @RequestParam long profileId,
        @RequestParam String slot,
        @RequestParam(required = false, defaultValue = "MEDIUM") String portion,
        @RequestParam String summary,
        @RequestParam(required = false) String location,
        @RequestParam(required = false) String description,
        @RequestParam("previewItems") String previewItemsJson,
        @RequestParam("image") MultipartFile image
    ) {
        mustOwn(userId, profileId);
        if (image == null || image.isEmpty()) {
            throw new ApiException(ApiError.BAD_REQUEST, "图片为空");
        }

        List<MealRecognitionExecutor.PreviewItem> previews;
        try {
            previews = json.readValue(previewItemsJson, new TypeReference<>() {});
        } catch (Exception e) {
            throw new ApiException(ApiError.BAD_REQUEST, "previewItems JSON 解析失败: " + e.getMessage());
        }

        // 1) 同步保存 meal
        MealEntity m = new MealEntity();
        m.setProfileId(profileId);
        m.setSlot(parseSlot(slot));
        m.setPortion(parsePortion(portion));
        m.setSource(MealEntity.Source.PHOTO);
        m.setSummary(summary == null ? "" : summary);
        m.setLocation(location == null ? "" : location.trim());
        m.setDescription(description == null ? "" : description.trim());
        m.setItemsJson(previewItemsJson == null ? "" : previewItemsJson);
        m.setRecognitionStatus(MealEntity.RecognitionStatus.PENDING);
        // 营养字段保持 0；veg_servings 用预览项粗算（蔬果 w/80）
        m.setVegServings(previews.stream()
            .filter(p -> "蔬果".equals(p.category()))
            .mapToInt(p -> p.weightG() / 80).sum());
        // 预填 waterMl（用 category 默认密度，避免 0；M3 识别完会覆盖）
        m.setWaterMl(previews.stream()
            .mapToInt(p -> estimatePreviewWater(p.category(), p.weightG()))
            .sum());
        // 用预览项粗算 kcal（kcal ≈ w*1.5, p ≈ w*0.05, f ≈ w*0.05, c ≈ w*0.2）→ 即时显示
        double baseKcal = 0, baseP = 0, baseF = 0, baseC = 0;
        for (var p : previews) {
            baseKcal += p.weightG() * 1.5;
            baseP += p.weightG() * 0.05;
            baseF += p.weightG() * 0.05;
            baseC += p.weightG() * 0.2;
        }
        m.setKcal((int) Math.round(baseKcal));
        m.setProteinG(round1(baseP));
        m.setFatG(round1(baseF));
        m.setCarbG(round1(baseC));
        m.setMealDate(LocalDate.now());
        // 截断到秒：Hibernate 6.5 + PG TIME 列对带微秒/纳秒的 LocalTime 反序列化时有 bug
        // （会算成 1秒-纳秒 的负值，导致 GET /v1/meals 整个列表 500 NanoOfSecond 异常）。
        // 餐次时间到秒足够，去掉小数秒从源头规避。
        m.setMealTime(LocalTime.now().truncatedTo(ChronoUnit.SECONDS));
        mealRepo.save(m);
        final long mealId = m.getId();

        // 2) 持久化图片
        try {
            Path dir = Paths.get(uploadDir);
            Files.createDirectories(dir);
            Path target = dir.resolve("meal_" + mealId + ".jpg");
            Files.write(target, image.getBytes());
        } catch (Exception e) {
            log.warn("[PR3] 图片持久化失败 mealId={}, err={}", mealId, e.getMessage());
        }

        // 3) 触发异步识别
        try {
            recognitionExecutor.enqueue(mealId, image.getBytes(), previews);
        } catch (Exception e) {
            log.warn("[PR3] enqueue 失败 mealId={}, err={}", mealId, e.getMessage());
        }

        return ApiResponse.ok(toDto(m));
    }

    private static int estimatePreviewWater(String category, int weightG) {
        if (category == null) return (int) (weightG * 0.5);
        return switch (category) {
            case "饮品" -> (int) (weightG * 0.95);
            case "蔬果" -> (int) (weightG * 0.85);
            case "主食" -> (int) (weightG * 0.5);
            case "蛋白" -> (int) (weightG * 0.65);
            case "坚果" -> (int) (weightG * 0.05);
            default -> (int) (weightG * 0.5);
        };
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@CurrentUserAnnotation long userId, @PathVariable long id) {
        MealEntity m = mealRepo.findById(id).orElseThrow(() -> new ApiException(ApiError.NOT_FOUND));
        mustOwn(userId, m.getProfileId());
        mealRepo.deleteById(id);
        return ApiResponse.ok(null);
    }

    private void mustOwn(long userId, long profileId) {
        var p = profileRepo.findById(profileId).orElseThrow(() -> new ApiException(ApiError.NOT_FOUND));
        if (p.getOwnerUserId() != userId && !java.util.Objects.equals(p.getManagedByUserId(), userId)) {
            throw new ApiException(ApiError.FORBIDDEN);
        }
    }

    private MealEntity.Slot parseSlot(String s) {
        if (s == null) return MealEntity.Slot.SNACK;
        try { return MealEntity.Slot.valueOf(s.toUpperCase()); }
        catch (Exception e) { return MealEntity.Slot.SNACK; }
    }
    private MealEntity.Portion parsePortion(String s) {
        if (s == null) return MealEntity.Portion.MEDIUM;
        try { return MealEntity.Portion.valueOf(s.toUpperCase()); }
        catch (Exception e) { return MealEntity.Portion.MEDIUM; }
    }
    private MealEntity.Source parseSource(String s) {
        if (s == null) return MealEntity.Source.MANUAL;
        try { return MealEntity.Source.valueOf(s.toUpperCase()); }
        catch (Exception e) { return MealEntity.Source.MANUAL; }
    }
    private MealDto toDto(MealEntity m) {
        return new MealDto(
            m.getId(), m.getProfileId(), m.getSlot().name(), m.getPortion().name(), m.getSource().name(),
            m.getSummary(), m.getKcal(), m.getProteinG(), m.getFatG(), m.getCarbG(),
            m.getFiberG(), m.getVegServings(), m.getWaterMl(),
            m.getRecognitionStatus() == null ? "DONE" : m.getRecognitionStatus().name(),
            m.getLocation(), m.getDescription(),
            m.getItemsJson() == null ? "" : m.getItemsJson(),
            m.getMealDate(), m.getMealTime()
        );
    }

    private static double round1(double v) { return Math.round(v * 10.0) / 10.0; }
}
