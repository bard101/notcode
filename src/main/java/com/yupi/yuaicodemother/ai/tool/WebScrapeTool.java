package com.yupi.yuaicodemother.ai.tool;

import cn.hutool.core.util.StrUtil;
import com.yupi.yuaicodemother.service.FirecrawlService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 网页抓取工具（供大模型调用）
 *
 * <p>AI 生成网页代码时，若用户提供了参考网址，可主动调用此工具抓取目标网页内容，
 * 参考其布局结构、配色风格、文案等，生成更贴近用户期望的代码。
 */
@Component
@Slf4j
public class WebScrapeTool {

    private static final int MAX_CONTENT_LENGTH = 8000;

    @Resource
    private FirecrawlService firecrawlService;

    /**
     * 抓取指定网页的内容，以 Markdown 格式返回。
     *
     * @param url 要抓取的网页 URL，必须是完整的 HTTP/HTTPS 地址
     * @return 网页的 Markdown 正文内容，可用于参考其结构、配色和文案
     */
    @Tool("抓取指定网页的内容并以 Markdown 格式返回。当用户提供了参考网址时，调用此工具获取网页内容，" +
            "参考该网页的布局、配色、交互风格来生成代码，使结果更贴近用户期望。")
    public String scrapeWebPage(@P("要抓取的网页完整 URL，例如 https://example.com") String url) {
        if (StrUtil.isBlank(url)) {
            return "URL 不能为空";
        }
        log.info("AI 调用网页抓取工具，url={}", url);
        String content = firecrawlService.scrapeToMarkdown(url);
        if (StrUtil.isBlank(content)) {
            return "无法抓取该网页内容，请检查 URL 是否正确或该网站是否允许爬取。";
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            content = content.substring(0, MAX_CONTENT_LENGTH) + "\n...[内容过长，已截断]";
        }
        return content;
    }
}
