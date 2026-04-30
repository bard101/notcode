package com.yupi.yuaicodemother.model.dto.firecrawl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FirecrawlScrapeResult {

    private boolean success;
    private FirecrawlData data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FirecrawlData {
        private String markdown;
        private String html;
        private FirecrawlMetadata metadata;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FirecrawlMetadata {
        private String title;
        private String description;
        private String sourceURL;
    }
}
