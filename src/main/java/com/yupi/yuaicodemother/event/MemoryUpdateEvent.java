package com.yupi.yuaicodemother.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 记忆更新事件
 * 由 AppServiceImpl 在 AI 消息落库后发布，
 * MemoryUpdateEventListener 监听后异步执行 L0/L1 记忆更新
 */
@Getter
public class MemoryUpdateEvent extends ApplicationEvent {

    /** 应用ID */
    private final Long appId;

    /** 用户ID */
    private final Long userId;

    /** 本轮用户消息（与 chat_history.title 相同） */
    private final String userMessage;

    /** 本轮对话的 chat_history 主键 ID（用于 L0 索引追加） */
    private final Long chatHistoryId;

    public MemoryUpdateEvent(Object source, Long appId, Long userId, String userMessage, Long chatHistoryId) {
        super(source);
        this.appId = appId;
        this.userId = userId;
        this.userMessage = userMessage;
        this.chatHistoryId = chatHistoryId;
    }
}
