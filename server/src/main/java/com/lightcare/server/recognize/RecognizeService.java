package com.lightcare.server.recognize;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lightcare.server.common.ApiError;
import com.lightcare.server.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 拍照识别（MiniMax-M3）核心逻辑。
 * PR3: 从 RecognizeController 抽出，给同步接口和 MealRecognitionExecutor 复用。
 */
@Service
public class RecognizeService {

    private static final String M3_URL = "https://api.minimaxi.com/anthropic/v1/messages";
    private static final String M3_MODEL = "MiniMax-M3";

    /** PR-Recipe: prompt 增 recipe 字段（识别时顺便输出做法，没把握就空）。 */
    private static final String PROMPT =
        "你是一个精确的食物营养分析助手。请仔细看图，识别图中所有食物，" +
        "并对每一项给出【基于图片可见部分的份量】的营养估算。\n" +
        "严格按下面 JSON 格式回答，不要任何解释、不要 markdown 代码块：\n" +
        "{\n" +
        "  \"items\": [\n" +
        "    {\n" +
        "      \"name\": \"食物中文名（简洁，如 白米饭 / 西红柿炒鸡蛋）\",\n" +
        "      \"category\": \"主食|蛋白|蔬果|饮品|坚果|其他\",\n" +
        "      \"weight_g\": 数字,  // 估算本份克数（整型）\n" +
        "      \"kcal\": 数字,      // 本份总热量（整型）\n" +
        "      \"protein_g\": 数字, // 蛋白克数（保留 1 位小数）\n" +
        "      \"fat_g\": 数字,     // 脂肪克数（保留 1 位小数）\n" +
        "      \"carb_g\": 数字,    // 碳水克数（保留 1 位小数）\n" +
        "      \"fiber_g\": 数字,   // 膳食纤维（保留 1 位小数，没有就给 0）\n" +
        "      \"sugar_g\": 数字,   // 糖分（保留 1 位小数，没有就给 0）\n" +
        "      \"sodium_mg\": 数字, // 钠毫克（整型）\n" +
        "      \"water_ml\": 数字,  // 本份含水量毫升（整型：汤/粥/饮料 自由水 + 食材本身含水；米饭 50ml, 蔬果按 80% 估, 干粮 20ml）\n" +
        "      \"confidence\": 0-1之间的数字, // 你对识别的确信度\n" +
        "      \"recipe\": { // 做法（可选；没把握就全部留空数组，client 会显示“无做法”）\n" +
        "        \"cooking_minutes\": 数字,   // 烹饪时间（分钟，整型）\n" +
        "        \"difficulty\": \"EASY|MEDIUM|HARD\",\n" +
        "        \"ingredients\": [{\"name\": \"食材名\", \"amount\": \"用量（如 2 个 / 100g / 适量）\"}],\n" +
        "        \"seasonings\":  [{\"name\": \"调料名\", \"amount\": \"用量（如 1 勺 / 5g / 少许）\"}],\n" +
        "        \"steps\": [{\"order\": 1, \"text\": \"第 1 步说明\"}, {\"order\": 2, \"text\": \"第 2 步说明\"}]\n" +
        "      }\n" +
        "    }\n" +
        "  ],\n" +
        "  \"total_kcal\": 数字\n" +
        "}\n" +
        "如果图片里没有食物，items 是空数组。";

    @Value("${M3_API_KEY:}")
    private String apiKey;

    private final HttpClient http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();
    private final ObjectMapper json = new ObjectMapper();

    /**
     * 调 M3 识别图片字节，返回 RecognizedItem 列表（PR3 加 waterMl 字段）。
     * @param imageBytes jpg/png/gif/webp
     * @param contentType 客户端声明的 mime；为 null 时按 magic bytes 推断
     */
    public List<RecognizedItem> recognizeBytes(byte[] imageBytes, String contentType) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ApiException(ApiError.SERVICE_UNAVAILABLE,
                "服务器未配置 M3_API_KEY");
        }
        if (imageBytes == null || imageBytes.length == 0) {
            throw new ApiException(ApiError.BAD_REQUEST, "图片为空");
        }
        if (imageBytes.length > 10 * 1024 * 1024) {
            throw new ApiException(ApiError.BAD_REQUEST, "图片超过 10MB");
        }
        try {
            String b64 = Base64.getEncoder().encodeToString(imageBytes);
            String mediaType = detectMediaType(contentType, imageBytes);

            String body = String.format(
                "{\"model\":\"%s\",\"max_tokens\":2048," +
                "\"messages\":[{\"role\":\"user\",\"content\":[" +
                "{\"type\":\"image\",\"source\":{\"type\":\"base64\",\"media_type\":\"%s\",\"data\":\"%s\"}}," +
                "{\"type\":\"text\",\"text\":%s}]}]}",
                M3_MODEL, mediaType, b64, json.writeValueAsString(PROMPT)
            );

            HttpRequest req = HttpRequest.newBuilder(URI.create(M3_URL))
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("content-type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new ApiException(ApiError.UPSTREAM_ERROR,
                    "M3 返回 " + resp.statusCode() + ": " + truncate(resp.body(), 300));
            }
            return parseItems(resp.body());
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(ApiError.UPSTREAM_ERROR, "识别失败: " + e.getMessage());
        }
    }

    /** 解析 M3 返回的 Anthropic 格式 → RecognizedItem。 */
    private List<RecognizedItem> parseItems(String body) throws Exception {
        JsonNode root = json.readTree(body);
        String content = "";
        for (JsonNode block : root.path("content")) {
            if ("text".equals(block.path("type").asText(""))) {
                content = block.path("text").asText("");
                break;
            }
        }
        if (content.isBlank()) {
            return List.of();
        }
        String clean = content.trim();
        if (clean.startsWith("```")) {
            clean = clean.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("```$", "").trim();
        }
        JsonNode parsed;
        try {
            parsed = json.readTree(clean);
        } catch (Exception e) {
            throw new ApiException(ApiError.UPSTREAM_ERROR, "M3 返回非 JSON: " + truncate(clean, 200));
        }
        List<RecognizedItem> items = new ArrayList<>();
        for (JsonNode it : parsed.path("items")) {
            // PR-Recipe: M3 顺便返回的 recipe（没填时给空，client 走"无做法"路径）
            RecipeHint hint = parseRecipeHint(it.path("recipe"));
            items.add(new RecognizedItem(
                it.path("name").asText(""),
                it.path("category").asText("其他"),
                it.path("weight_g").asInt(0),
                it.path("kcal").asInt(0),
                round1(it.path("protein_g").asDouble(0)),
                round1(it.path("fat_g").asDouble(0)),
                round1(it.path("carb_g").asDouble(0)),
                round1(it.path("fiber_g").asDouble(0)),
                round1(it.path("sugar_g").asDouble(0)),
                it.path("sodium_mg").asInt(0),
                it.path("water_ml").asInt(0),    // PR3: 加水字段
                clamp01(it.path("confidence").asDouble(0.0)),
                hint
            ));
        }
        return items;
    }

    /**
     * M3 输出的 recipe 提示（结构弱类型：M3 可能给不完整字段，client 拿到后存 DB）。
     * 没填（hint 为 null）→ 当作"无做法"。isEmpty 留作 client 判断是否值得展示。
     */
    public record RecipeHint(
        int cookingMinutes,
        String difficulty,         // EASY | MEDIUM | HARD | null
        List<Item> ingredients,     // 可能空
        List<Item> seasonings,
        List<Step> steps
    ) {
        public boolean isEmpty() {
            return cookingMinutes <= 0
                && (ingredients == null || ingredients.isEmpty())
                && (seasonings  == null || seasonings.isEmpty())
                && (steps       == null || steps.isEmpty());
        }
    }
    public record Item(String name, String amount) {}
    public record Step(int order, String text) {}

    /** 把 M3 返回的 recipe 节点解析成 RecipeHint（不抛异常；解析失败 → null）。 */
    private static RecipeHint parseRecipeHint(JsonNode r) {
        if (r == null || r.isMissingNode() || r.isNull()) return null;
        try {
            int minutes = r.path("cooking_minutes").asInt(0);
            String diff = r.path("difficulty").asText(null);
            if (diff != null && diff.isBlank()) diff = null;
            List<Item> ings = new ArrayList<>();
            for (JsonNode n : r.path("ingredients")) {
                ings.add(new Item(n.path("name").asText(""), n.path("amount").asText("")));
            }
            List<Item> seaso = new ArrayList<>();
            for (JsonNode n : r.path("seasonings")) {
                seaso.add(new Item(n.path("name").asText(""), n.path("amount").asText("")));
            }
            List<Step> steps = new ArrayList<>();
            for (JsonNode n : r.path("steps")) {
                steps.add(new Step(n.path("order").asInt(steps.size() + 1), n.path("text").asText("")));
            }
            return new RecipeHint(minutes, diff, ings, seaso, steps);
        } catch (Exception e) {
            return null;
        }
    }

    private static double round1(double v) { return Math.round(v * 10.0) / 10.0; }
    private static double clamp01(double v) {
        if (Double.isNaN(v)) return 0.0;
        if (v < 0) return 0;
        if (v > 1) return 1;
        return v;
    }

    private static String detectMediaType(String contentType, byte[] bytes) {
        if (contentType != null) {
            String ct = contentType.toLowerCase();
            if (ct.contains("png")) return "image/png";
            if (ct.contains("gif")) return "image/gif";
            if (ct.contains("webp")) return "image/webp";
            if (ct.contains("jpeg") || ct.contains("jpg")) return "image/jpeg";
        }
        if (bytes.length >= 4 &&
            (bytes[0] & 0xff) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
            return "image/png";
        }
        if (bytes.length >= 2 && (bytes[0] & 0xff) == 0xFF && (bytes[1] & 0xff) == 0xD8) {
            return "image/jpeg";
        }
        if (bytes.length >= 4 && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F') {
            return "image/gif";
        }
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
            && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "image/webp";
        }
        return "image/jpeg";
    }

    private String truncate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max) + "...";
    }

    /** 精确宏量营养识别结果（PR3 加 waterMl；PR-Recipe 加 recipeHint）。 */
    public record RecognizedItem(
        String name,
        String category,
        int weightG,
        int kcal,
        double proteinG,
        double fatG,
        double carbG,
        double fiberG,
        double sugarG,
        int sodiumMg,
        int waterMl,        // PR3: 本份含水（毫升）
        double confidence,
        RecipeHint recipeHint   // PR-Recipe: 做法提示（可能 null）
    ) {}
}
