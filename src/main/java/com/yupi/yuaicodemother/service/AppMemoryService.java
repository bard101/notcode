package com.yupi.yuaicodemother.service;

import cn.hutool.core.util.StrUtil;
import com.yupi.yuaicodemother.mapper.AppMapper;
import com.yupi.yuaicodemother.model.entity.App;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 应用记忆服务（三层记忆架构的语义层）
 * 负责读写 App 表中的需求摘要（L1 温数据，即 warm memory），
 * 避免注入 AppService 引发循环依赖
 */
@Service
@Slf4j
public class AppMemoryService {

    private static final String WARM_MEMORY_KEY_PREFIX = "mem:warm:";
    private static final Duration WARM_MEMORY_TTL = Duration.ofSeconds(7200);

    @Resource
    private AppMapper appMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 获取 L1 温数据摘要（app.requirementSummary），优先 Redis 缓存
     * 为空时返回空字符串
     */
    public String getRequirementSummary(Long appId) {
        if (appId == null || appId <= 0) {
            return "";
        }
        String redisKey = WARM_MEMORY_KEY_PREFIX + appId;
        try {
            String cached = stringRedisTemplate.opsForValue().get(redisKey);
            if (cached != null) {
                return cached;
            }
        } catch (Exception e) {
            log.warn("温数据 Redis 读取失败，降级查 DB，appId={}: {}", appId, e.getMessage());
        }
        App app = appMapper.selectOneById(appId);
        if (app == null || StrUtil.isBlank(app.getRequirementSummary())) {
            return "";
        }
        String summary = app.getRequirementSummary();
        try {
            stringRedisTemplate.opsForValue().set(redisKey, summary, WARM_MEMORY_TTL);
        } catch (Exception e) {
            log.warn("温数据回写 Redis 失败，appId={}: {}", appId, e.getMessage());
        }
        return summary;
    }

    /**
     * 使 L1 温数据 Redis 缓存失效（在 DB 更新后调用）
     */
    public void evictCache(Long appId) {
        if (appId == null || appId <= 0) {
            return;
        }
        try {
            stringRedisTemplate.delete(WARM_MEMORY_KEY_PREFIX + appId);
            log.debug("温数据缓存已失效，appId={}", appId);
        } catch (Exception e) {
            log.warn("温数据缓存失效失败，appId={}: {}", appId, e.getMessage());
        }
    }
}
