package com.yupi.yuaicodemother.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yupi.yuaicodemother.ai.tool.AppCodeFileTool;
import com.yupi.yuaicodemother.ai.tool.ImageSearchTool;
import com.yupi.yuaicodemother.ai.tool.WebScrapeTool;
import com.yupi.yuaicodemother.constant.AppConstant;
import com.yupi.yuaicodemother.mapper.AppMapper;
import com.yupi.yuaicodemother.model.entity.App;
import com.yupi.yuaicodemother.service.ChatHistoryService;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AI 服务创建工厂
 */
@Configuration
@Slf4j
public class AiCodeGeneratorServiceFactory {

    @Resource
    private ChatModel chatModel;

    @Resource
    private StreamingChatModel streamingChatModel;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private AppMapper appMapper;

    @Resource
    private ImageSearchTool imageSearchTool;

    @Resource
    private WebScrapeTool webScrapeTool;

    /**
     * Redis 是否支持 JSON 模块（探测结果，启动时确定）
     * false 时退化为 InMemoryChatMemoryStore + MySQL 加载
     */
    private final AtomicBoolean redisJsonAvailable = new AtomicBoolean(true);

    /**
     * 启动时探测 Redis 是否支持 JSON.GET
     */
    @PostConstruct
    public void probeRedisJson() {
        try {
            // 探测一个不存在的 key，JSON 模块存在时返回 null，不存在时抛 JedisDataException
            redisChatMemoryStore.getMessages(Long.MIN_VALUE);
            log.info("Redis JSON 模块可用，使用 RedisChatMemoryStore 持久化对话记忆");
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("JSON.GET")) {
                redisJsonAvailable.set(false);
                log.warn("Redis 不支持 JSON 模块，退化为 InMemoryChatMemoryStore（对话记忆不跨重启持久化）");
            } else {
                // 其他 Redis 错误（如连接失败）也降级，避免阻断启动
                redisJsonAvailable.set(false);
                log.warn("Redis 探测失败，退化为 InMemoryChatMemoryStore: {}", e.getMessage());
            }
        }
    }

    /**
     * AI 服务实例缓存（per appId）
     * 缓存策略：
     * - 最大缓存 1000 个实例
     * - 写入后 30 分钟过期
     * - 访问后 10 分钟过期
     */
    private final Cache<Long, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI 服务实例被移除，appId: {}, 原因: {}", key, cause);
            })
            .build();

    /**
     * 根据 appId 获取 AI 服务（有缓存则复用，缓存不命中则新建）
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
        return serviceCache.get(appId, this::createAiCodeGeneratorService);
    }

    /**
     * 创建新的 AI 服务实例（per appId）
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(long appId) {
        log.info("为 appId: {} 创建新的 AI 服务实例", appId);

        MessageWindowChatMemory chatMemory;
        if (redisJsonAvailable.get()) {
            // Redis JSON 可用：用 RedisChatMemoryStore 持久化对话记忆
            chatMemory = MessageWindowChatMemory.builder()
                    .id(appId)
                    .chatMemoryStore(redisChatMemoryStore)
                    .maxMessages(10)
                    .build();
            // 冷启动：Redis 无历史时从 MySQL 加载
            if (appId > 0) {
                try {
                    List<ChatMessage> existingMessages = redisChatMemoryStore.getMessages(appId);
                    if (existingMessages == null || existingMessages.isEmpty()) {
                        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 10);
                    }
                } catch (Exception e) {
                    log.warn("Redis 读取历史失败，降级从 MySQL 加载，appId={}: {}", appId, e.getMessage());
                    chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 10);
                }
            }
        } else {
            // Redis JSON 不可用：用 InMemoryChatMemoryStore，从 MySQL 加载历史
            InMemoryChatMemoryStore inMemoryStore = new InMemoryChatMemoryStore();
            chatMemory = MessageWindowChatMemory.builder()
                    .id(appId)
                    .chatMemoryStore(inMemoryStore)
                    .maxMessages(10)
                    .build();
            if (appId > 0) {
                chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 10);
            }
        }

        var builder = AiServices.builder(AiCodeGeneratorService.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .chatMemory(chatMemory);

        // 为真实 App 注册代码文件操作工具 + 图片搜索工具
        if (appId > 0) {
            App app = appMapper.selectOneById(appId);
            if (app != null) {
                AppCodeFileTool tool = new AppCodeFileTool(appId, app.getCodeGenType(), AppConstant.CODE_OUTPUT_ROOT_DIR);
                builder.tools(tool, imageSearchTool, webScrapeTool);
            }
        }

        return builder.build();
    }

    /**
     * 默认 AI 代码生成服务 Bean（appId=0，用于 Spring 上下文，不含工具）
     */
    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        return getAiCodeGeneratorService(0);
    }

    /**
     * 记忆提取服务 Bean（无对话记忆、无工具，专用于摘要/偏好提取）
     */
    @Bean
    public MemoryExtractorService memoryExtractorService() {
        return AiServices.builder(MemoryExtractorService.class)
                .chatModel(chatModel)
                .build();
    }
}

