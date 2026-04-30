package com.yupi.yuaicodemother.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * AI 记忆提取服务接口（无状态 Bean）
 * 负责三层记忆架构的 AI 语义处理：
 *   - L1 温数据摘要的首次生成和滚动更新
 *   - L2 对话记录的 ai_summary 生成
 */
public interface MemoryExtractorService {

    /**
     * 首次生成 L1 温数据摘要（历史为空时使用）
     *
     * @param recentHistory 最近几轮对话摘要文本（格式：用户：xxx\nAI摘要：xxx）
     * @return 结构化温数据摘要（300字内）
     */
    @SystemMessage(fromResource = "prompt/condense-warm-memory-first.txt")
    @UserMessage("对话历史：\n{{recentHistory}}")
    String condenseSummaryFresh(@V("recentHistory") String recentHistory);

    /**
     * 滚动更新 L1 温数据摘要（已有历史摘要时使用）
     *
     * @param prevSummary   上一次生成的温数据摘要
     * @param recentHistory 最近几轮对话摘要文本
     * @return 更新后的温数据摘要（300字内）
     */
    @SystemMessage(fromResource = "prompt/condense-warm-memory-update.txt")
    @UserMessage("请根据已有摘要和最近对话，更新温数据摘要。")
    String condenseSummary(@V("prevSummary") String prevSummary, @V("recentHistory") String recentHistory);

    /**
     * 生成本轮对话的 AI 摘要（存入 chat_history.ai_summary，供 L1 更新使用）
     *
     * @param userMessage 用户的提问
     * @param aiCode      AI 生成的代码内容（可能很长）
     * @return 一句话摘要（不超过 20 字）
     */
    @SystemMessage(fromResource = "prompt/summarize-ai-response.txt")
    @UserMessage("用户提问：{{userMessage}}\n\nAI生成内容（节选）：{{aiCode}}")
    String generateAiSummary(@V("userMessage") String userMessage, @V("aiCode") String aiCode);
}
