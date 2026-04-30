package com.yupi.yuaicodemother.event;

import cn.hutool.core.util.StrUtil;
import com.yupi.yuaicodemother.ai.MemoryExtractorService;
import com.yupi.yuaicodemother.mapper.AppMapper;
import com.yupi.yuaicodemother.model.entity.App;
import com.yupi.yuaicodemother.service.AppMemoryService;
import com.yupi.yuaicodemother.service.ChatHistoryService;
import com.yupi.yuaicodemother.service.MemoryIndexService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@Slf4j
public class MemoryUpdateEventListener {

    private static final String REFLECT_COUNTER_PREFIX = "mem:reflect:counter:";
    private static final int REFLECT_THRESHOLD = 3;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private MemoryExtractorService memoryExtractorService;

    @Resource
    private MemoryIndexService memoryIndexService;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private AppMemoryService appMemoryService;

    @Resource
    private AppMapper appMapper;

    @Async("memoryTaskExecutor")
    @EventListener
    public void onMemoryUpdateEvent(MemoryUpdateEvent event) {
        Long appId = event.getAppId();
        Long chatHistoryId = event.getChatHistoryId();
        String userMessage = event.getUserMessage();
        log.debug("L0 start appId={}", appId);
        try {
            memoryIndexService.appendRequest(appId, chatHistoryId, userMessage);
            String counterKey = REFLECT_COUNTER_PREFIX + appId;
            Long count = stringRedisTemplate.opsForValue().increment(counterKey);
            stringRedisTemplate.expire(counterKey, Duration.ofDays(7));
            if (count == null || count < REFLECT_THRESHOLD) {
                return;
            }
            stringRedisTemplate.delete(counterKey);
            updateWarmMemory(appId);
        } catch (Exception e) {
            log.warn("memory update failed appId={}: {}", appId, e.getMessage());
        }
    }

    private void updateWarmMemory(Long appId) {
        try {
            String recentHistory = chatHistoryService.getLastNRoundsText(appId, 3);
            if (StrUtil.isBlank(recentHistory)) return;
            String prevSummary = appMemoryService.getRequirementSummary(appId);
            String newSummary;
            if (StrUtil.isBlank(prevSummary)) {
                newSummary = memoryExtractorService.condenseSummaryFresh(recentHistory);
            } else {
                newSummary = memoryExtractorService.condenseSummary(prevSummary, recentHistory);
            }
            if (StrUtil.isBlank(newSummary)) return;
            App update = new App();
            update.setId(appId);
            update.setRequirementSummary(newSummary);
            appMapper.update(update);
            appMemoryService.evictCache(appId);
        } catch (Exception e) {
            log.warn("L1 update failed appId={}: {}", appId, e.getMessage());
        }
    }
}
