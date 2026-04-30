package com.yupi.yuaicodemother.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.yupi.yuaicodemother.model.dto.chathistory.ChatHistoryQueryRequest;
import com.yupi.yuaicodemother.model.entity.ChatHistory;
import com.yupi.yuaicodemother.model.entity.User;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.time.LocalDateTime;

/**
 * 对话历史 服务层。
 * 新架构：每次请求一条记录（title=用户提问，message=完整一轮对话，ai_summary=AI摘要）
 *
 * @author <a >Klong</a>
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 保存一轮完整对话
     *
     * @param appId      应用 ID
     * @param userId     用户 ID
     * @param userMessage 用户原始提问（存入 title 字段）
     * @param aiResponse  AI 完整回复（与 userMessage 合并存入 message 字段）
     * @param aiSummary  AI 回复一句话摘要（存入 ai_summary 字段）
     * @return 新插入记录的 ID（供 L0 索引使用）
     */
    Long saveChatHistory(Long appId, Long userId, String userMessage, String aiResponse, String aiSummary);

    /**
     * 获取最近 N 轮对话的摘要文本，用于 L1 温数据更新
     * 格式：用户：{title}\nAI摘要：{ai_summary}\n\n（多轮换行分隔）
     *
     * @param appId 应用 ID
     * @param n     轮数
     * @return 格式化的摘要文本，无数据时返回空字符串
     */
    String getLastNRoundsText(Long appId, int n);

    /**
     * 根据应用 id 删除对话历史
     *
     * @param appId 应用 ID
     * @return 是否成功
     */
    boolean deleteByAppId(Long appId);

    /**
     * 分页查询某 APP 的对话记录
     *
     * @param appId          应用 ID
     * @param pageSize       每页大小
     * @param lastCreateTime 游标时间
     * @param loginUser      当前登录用户
     * @return 分页结果
     */
    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                               LocalDateTime lastCreateTime,
                                               User loginUser);

    /**
     * 加载对话历史到 LangChain4j 的 MessageWindowChatMemory
     * 将每条记录拆分为 UserMessage（title）和 AiMessage（message 中 AI 部分）
     *
     * @param appId      应用 ID
     * @param chatMemory 目标内存对象
     * @param maxCount   最多加载多少轮
     * @return 加载成功的条数
     */
    int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);

    /**
     * 构造查询条件
     *
     * @param chatHistoryQueryRequest 查询请求
     * @return QueryWrapper
     */
    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);
}
