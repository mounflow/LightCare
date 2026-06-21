package com.lightcare.server.recognize;

import com.lightcare.server.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 拍照食物识别（同步入口，PR3 起内部走 RecognizeService）。
 *
 * 旧 POST /v1/recognize（multipart，字段 image=图片文件） → service → 返回。
 * PR3 异步路径：POST /v1/meals/photo。
 */
@RestController
@RequestMapping("/v1/recognize")
@RequiredArgsConstructor
public class RecognizeController {

    private final RecognizeService service;

    @PostMapping(consumes = "multipart/form-data")
    public ApiResponse<List<RecognizedItem>> recognize(@RequestParam("image") MultipartFile image) {
        try {
            byte[] bytes = image.getBytes();
            // service 内部用 RecognizeService.RecognizedItem，转成 controller 旧 record 给老客户端。
            List<RecognizeService.RecognizedItem> src = service.recognizeBytes(bytes, image.getContentType());
            List<RecognizedItem> dst = src.stream().map(s -> new RecognizedItem(
                s.name(), s.category(), s.weightG(), s.kcal(),
                s.proteinG(), s.fatG(), s.carbG(), s.fiberG(), s.sugarG(), s.sodiumMg(), s.confidence(),
                s.recipeHint() == null ? null : new RecipeHintDto(
                    s.recipeHint().cookingMinutes(),
                    s.recipeHint().difficulty(),
                    s.recipeHint().ingredients() == null ? null : s.recipeHint().ingredients().stream()
                        .map(i -> new ItemDto(i.name(), i.amount())).toList(),
                    s.recipeHint().seasonings() == null ? null : s.recipeHint().seasonings().stream()
                        .map(i -> new ItemDto(i.name(), i.amount())).toList(),
                    s.recipeHint().steps() == null ? null : s.recipeHint().steps().stream()
                        .map(st -> new StepDto(st.order(), st.text())).toList()
                )
            )).toList();
            return ApiResponse.ok(dst);
        } catch (com.lightcare.server.common.ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new com.lightcare.server.common.ApiException(
                com.lightcare.server.common.ApiError.UPSTREAM_ERROR, "识别失败: " + e.getMessage());
        }
    }

    /** 兼容旧 DTO（不包含 waterMl / recipe，老客户端无感）。 */
    public record RecognizedItem(
        String name, String category, int weightG, int kcal,
        double proteinG, double fatG, double carbG, double fiberG, double sugarG,
        int sodiumMg, double confidence,
        RecipeHintDto recipe
    ) {}

    public record RecipeHintDto(
        int cookingMinutes, String difficulty,
        List<ItemDto> ingredients, List<ItemDto> seasonings, List<StepDto> steps
    ) {}
    public record ItemDto(String name, String amount) {}
    public record StepDto(int order, String text) {}
}
