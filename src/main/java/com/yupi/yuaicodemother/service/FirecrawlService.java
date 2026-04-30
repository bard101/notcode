package com.yupi.yuaicodemother.service;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yupi.yuaicodemother.model.dto.firecrawl.FirecrawlScrapeRequest;
import com.yupi.yuaicodemother.model.dto.firecrawl.FirecrawlScrapeResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Firecrawl 网页抓取服务
 * 文档: https://docs.firecrawl.dev/api-reference/endpoint/scrape
 */
@Service
@Slf4j
public class FirecrawlService {

    @Value("${firecrawl.api-key}")
    private String apiKey;

    @Value("${firecrawl.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 抓取指定 URL 的网页内容，返回 Markdown 格式文本
     *
     * @param url 目标网页 URL
     * @return Markdown 格式的网页正文，失败时返回空字符串
     */
    public String scrapeToMarkdown(String url) {
        FirecrawlScrapeResult result = scrape(url, List.of("markdown"));
        if (result == null || !result.isSuccess() || result.getData() == null) {
            return "";
        }
        return StrUtil.nullToEmpty(result.getData().getMarkdown());
    }

    /**
     * 抓取指定 URL 的网页内容
     *
     * @param url     目标网页 URL
     * @param formats 返回格式，如 ["markdown"] 或 ["html"]
     * @return FirecrawlScrapeResult，失败时返回 null
     */
    public FirecrawlScrapeResult scrape(String url, List<String> formats) {
        if (StrUtil.isBlank(url)) {
            return null;
        }
        try {
            FirecrawlScrapeRequest request = FirecrawlScrapeRequest.builder()
                    .url(url)
                    .formats(formats)
                    .onlyMainContent(true)
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            String body = objectMapper.writeValueAsString(request);
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            String response = restTemplate.postForObject(
                    baseUrl + "/scrape", entity, String.class);

            return objectMapper.readValue(response, FirecrawlScrapeResult.class);
        } catch (Exception e) {
            log.warn("Firecrawl 抓取失败，url={}: {}", url, e.getMessage());
            return null;
        }
    }
}

