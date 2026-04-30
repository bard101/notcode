package com.yupi.yuaicodemother.model.dto.firecrawl;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class FirecrawlScrapeRequest {

    /** 要抓取的 URL */
    private String url;

    /** 返回格式，如 ["markdown", "html"] */
    private List<String> formats;

    /** 是否只提取正文（去除导航、页脚等噪声） */
    @Builder.Default
    private boolean onlyMainContent = true;

    /** 超时（毫秒），默认 30000 */
    @Builder.Default
    private int timeout = 30000;
}
