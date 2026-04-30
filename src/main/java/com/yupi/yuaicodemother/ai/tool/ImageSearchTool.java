package com.yupi.yuaicodemother.ai.tool;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 图片搜索工具（基于 Pexels API）
 *
 * <p>AI 生成网页代码时，调用此工具按关键词搜索真实图片 URL，
 * 避免使用随机占位图（picsum.photos），提升网页视觉相关性和专业感。
 *
 * <p>Pexels 免费套餐：20,000 次/月，200 次/小时，无需署名限制。
 * 注册地址：https://www.pexels.com/api/
 */
@Component
@Slf4j
public class ImageSearchTool {

    private static final String PEXELS_API_URL = "https://api.pexels.com/v1/search";
    private static final String FALLBACK_URL_TEMPLATE = "https://picsum.photos/%d/%d?random=%d";

    @Value("${pexels.api-key:}")
    private String apiKey;

    /**
     * 根据关键词搜索相关图片，返回图片 URL 列表。
     *
     * @param keywords 图片描述关键词，建议用英文以获得更好结果，例如 "modern office building"
     * @param width    期望的图片宽度（像素），例如 800
     * @param height   期望的图片高度（像素），例如 600
     * @param count    需要的图片数量，建议 1-5
     * @return 图片 URL 数组的 JSON 字符串，如 ["https://...", "https://..."]
     */
    @Tool("根据关键词搜索相关图片，返回真实图片的URL列表。每当网页中需要图片时，必须调用此工具而非使用 picsum.photos 随机图片。返回的URL可直接用于 <img src=\"...\">。")
    public String searchImages(
            @P("图片描述关键词，用英文，例如: modern technology office, nature landscape, team meeting") String keywords,
            @P("图片宽度(px)，例如 800") int width,
            @P("图片高度(px)，例如 600") int height,
            @P("需要的图片数量，1-5之间") int count) {

        // 数量范围保护
        int safeCount = Math.max(1, Math.min(count, 5));

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Pexels API key 未配置，返回占位图 URL");
            return buildFallbackUrls(width, height, safeCount);
        }

        try {
            HttpResponse response = HttpRequest.get(PEXELS_API_URL)
                    .header("Authorization", apiKey)
                    .form("query", keywords)
                    .form("per_page", safeCount)
                    .form("orientation", width >= height ? "landscape" : "portrait")
                    .timeout(8000)
                    .execute();

            if (!response.isOk()) {
                log.warn("Pexels API 响应异常: status={}, keywords={}", response.getStatus(), keywords);
                return buildFallbackUrls(width, height, safeCount);
            }

            JSONObject body = JSONUtil.parseObj(response.body());
            JSONArray photos = body.getJSONArray("photos");
            if (photos == null || photos.isEmpty()) {
                log.debug("Pexels 没有找到图片: keywords={}", keywords);
                return buildFallbackUrls(width, height, safeCount);
            }

            List<String> urls = new ArrayList<>();
            for (int i = 0; i < Math.min(photos.size(), safeCount); i++) {
                JSONObject photo = photos.getJSONObject(i);
                JSONObject src = photo.getJSONObject("src");
                // 优先用 large (宽度约 940px)，其次 medium，最后 original
                String url = src.getStr("large", src.getStr("medium", src.getStr("original")));
                if (url != null && !url.isBlank()) {
                    urls.add(url);
                }
            }

            if (urls.isEmpty()) {
                return buildFallbackUrls(width, height, safeCount);
            }

            log.debug("Pexels 搜索成功: keywords={}, 返回 {} 张图片", keywords, urls.size());
            return JSONUtil.toJsonStr(urls);

        } catch (Exception e) {
            log.warn("Pexels API 调用失败: keywords={}, error={}", keywords, e.getMessage());
            return buildFallbackUrls(width, height, safeCount);
        }
    }

    /**
     * 构建 picsum 占位图 URL 列表（API 不可用时的降级）
     */
    private String buildFallbackUrls(int width, int height, int count) {
        List<String> urls = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            urls.add(String.format(FALLBACK_URL_TEMPLATE, width, height, (int) (Math.random() * 1000) + i));
        }
        return JSONUtil.toJsonStr(urls);
    }
}
