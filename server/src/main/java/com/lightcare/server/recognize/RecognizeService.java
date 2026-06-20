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

    /** PR3: 加 water_ml（汤/粥/饮料估毫升，整型；食材本身含水也算）。 */
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
        "      \"confidence\": 0-1之间的数字  // 你对识别的确信度\n" +
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
                clamp01(it.path("confidence").asDouble(0.0))
            ));
        }
        return items;
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

    /** 精确宏量营养识别结果（PR3 加 waterMl）。 */
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
        double confidence
    ) {}
}
