package com.yupi.yuaicodemother.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yupi.yuaicodemother.constant.UserConstant;
import com.yupi.yuaicodemother.exception.ErrorCode;
import com.yupi.yuaicodemother.exception.ThrowUtils;
import com.yupi.yuaicodemother.model.dto.chathistory.ChatHistoryQueryRequest;
import com.yupi.yuaicodemother.model.entity.App;
import com.yupi.yuaicodemother.model.entity.ChatHistory;
import com.yupi.yuaicodemother.mapper.ChatHistoryMapper;
import com.yupi.yuaicodemother.model.entity.User;
import com.yupi.yuaicodemother.service.AppService;
import com.yupi.yuaicodemother.service.ChatHistoryService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService {

    public static final String AI_RESPONSE_SEPARATOR = "\n\n===AI_RESPONSE===\n";

    @Resource
    @Lazy
    private AppService appService;

    @Override
    public Long saveChatHistory(Long appId, Long userId, String userMessage, String aiResponse, String aiSummary) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(userMessage), ErrorCode.PARAMS_ERROR, "用户提问不能为空");
        String fullMessage = userMessage + AI_RESPONSE_SEPARATOR + (aiResponse == null ? "" : aiResponse);
        ChatHistory chatHistory = ChatHistory.builder()
                .appId(appId)
                .userId(userId)
                .title(userMessage)
                .message(fullMessage)
                .aiSummary(aiSummary)
                .build();
        this.save(chatHistory);
        return chatHistory.getId();
    }

    @Override
    public String getLastNRoundsText(Long appId, int n) {
        if (appId == null || appId <= 0 || n <= 0) {
            return "";
        }
        List<ChatHistory> historyList = this.list(QueryWrapper.create()
                .eq(ChatHistory::getAppId, appId)
                .orderBy(ChatHistory::getCreateTime, false)
                .limit(1, n));
        if (CollUtil.isEmpty(historyList)) {
            return "";
        }
        historyList = historyList.reversed();
        StringBuilder sb = new StringBuilder();
        for (ChatHistory history : historyList) {
            sb.append("用户：").append(history.getTitle()).append("\n");
            String summary = StrUtil.isBlank(history.getAiSummary()) ? "（无摘要）" : history.getAiSummary();
            sb.append("AI摘要：").append(summary).append("\n\n");
        }
        return sb.toString().trim();
    }

    @Override
    public boolean deleteByAppId(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", appId);
        return this.remove(queryWrapper);
    }

    @Override
    public Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                                      LocalDateTime lastCreateTime,
                                                      User loginUser) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 50, ErrorCode.PARAMS_ERROR, "页面大小必须在1-50之间");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        boolean isCreator = app.getUserId().equals(loginUser.getId());
        ThrowUtils.throwIf(!isAdmin && !isCreator, ErrorCode.NO_AUTH_ERROR, "无权查看该应用的对话历史");
        ChatHistoryQueryRequest queryRequest = new ChatHistoryQueryRequest();
        queryRequest.setAppId(appId);
        queryRequest.setLastCreateTime(lastCreateTime);
        QueryWrapper queryWrapper = this.getQueryWrapper(queryRequest);
        return this.page(Page.of(1, pageSize), queryWrapper);
    }

    @Override
    public int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount) {
        try {
            List<ChatHistory> historyList = this.list(QueryWrapper.create()
                    .eq(ChatHistory::getAppId, appId)
                    .orderBy(ChatHistory::getCreateTime, false)
                    .limit(1, maxCount));
            if (CollUtil.isEmpty(historyList)) {
                return 0;
            }
            historyList = historyList.reversed();
            chatMemory.clear();
            int loadedCount = 0;
            for (ChatHistory history : historyList) {
                if (StrUtil.isNotBlank(history.getTitle())) {
                    chatMemory.add(UserMessage.from(history.getTitle()));
                }
                String msg = history.getMessage();
                if (StrUtil.isNotBlank(msg)) {
                    int sepIdx = msg.indexOf(AI_RESPONSE_SEPARATOR);
                    String aiPart = sepIdx >= 0
                            ? msg.substring(sepIdx + AI_RESPONSE_SEPARATOR.length())
                            : msg;
                    if (StrUtil.isNotBlank(aiPart)) {
                        chatMemory.add(AiMessage.from(aiPart));
                    }
                }
                loadedCount++;
            }
            log.info("成功为 appId: {} 加载 {} 轮历史消息", appId, loadedCount);
            return loadedCount;
        } catch (Exception e) {
            log.error("加载历史对话失败，appId: {}, error: {}", appId, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        if (chatHistoryQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chatHistoryQueryRequest.getId();
        String message = chatHistoryQueryRequest.getMessage();
        Long appId = chatHistoryQueryRequest.getAppId();
        Long userId = chatHistoryQueryRequest.getUserId();
        LocalDateTime lastCreateTime = chatHistoryQueryRequest.getLastCreateTime();
        String sortField = chatHistoryQueryRequest.getSortField();
        String sortOrder = chatHistoryQueryRequest.getSortOrder();
        queryWrapper.eq("id", id)
                .like("message", message)
                .eq("appId", appId)
                .eq("userId", userId);
        if (lastCreateTime != null) {
            queryWrapper.lt("createTime", lastCreateTime);
        }
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
        } else {
            queryWrapper.orderBy("createTime", false);
        }
        return queryWrapper;
    }
}
