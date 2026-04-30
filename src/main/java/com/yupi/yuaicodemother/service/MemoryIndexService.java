package com.yupi.yuaicodemother.service;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yupi.yuaicodemother.mapper.MemIndexSnapshotMapper;
import com.yupi.yuaicodemother.model.entity.MemIndexSnapshot;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * L0 记忆索引服务（请求索引，每次请求追加）
 * 索引字符串格式（换行分隔，最多保留 50 条）：
 *   {chatHistoryId}:{用户完整提问}\n{chatHistoryId}:{用户完整提问}\n...
 *
 * Redis key：mem:index:{appId}，TTL 3600s
 */
@Service
@Slf4j
public class MemoryIndexService extends ServiceImpl<MemIndexSnapshotMapper, MemIndexSnapshot> {

    private static final String REDIS_KEY_PREFIX = "mem:index:";
    private static final Duration REDIS_TTL = Duration.ofSeconds(3600);
    private static final int MAX_ENTRIES = 50;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public String getIndexData(Long appId) {
        if (appId == null || appId <= 0) {
            return "";
        }
        String redisKey = REDIS_KEY_PREFIX + appId;
        try {
            String cached = stringRedisTemplate.opsForValue().get(redisKey);
            if (StrUtil.isNotBlank(cached)) {
                return cached;
            }
        } catch (Exception e) {
            log.warn("L0 索引 Redis 读取失败，降级查 MySQL，appId={}: {}", appId, e.getMessage());
        }
        MemIndexSnapshot snapshot = this.getOne(
                QueryWrapper.create().eq(MemIndexSnapshot::getAppId, appId)
        );
        if (snapshot == null || StrUtil.isBlank(snapshot.getIndexData())) {
            return "";
        }
        try {
            stringRedisTemplate.opsForValue().set(redisKey, snapshot.getIndexData(), REDIS_TTL);
        } catch (Exception e) {
            log.warn("L0 索引回写 Redis 失败，appId={}: {}", appId, e.getMessage());
        }
        return snapshot.getIndexData();
    }

    public void appendRequest(Long appId, Long chatHistoryId, String userMessage) {
        if (appId == null || appId <= 0 || chatHistoryId == null || StrUtil.isBlank(userMessage)) {
            return;
        }
        try {
            String existing = getIndexData(appId);
            List<String> lines = new ArrayList<>();
            if (StrUtil.isNotBlank(existing)) {
                lines = Arrays.stream(existing.split("\n"))
                        .filter(StrUtil::isNotBlank)
                        .collect(Collectors.toCollection(ArrayList::new));
            }
            lines.add(chatHistoryId + ":" + userMessage);
            if (lines.size() > MAX_ENTRIES) {
                lines = lines.subList(lines.size() - MAX_ENTRIES, lines.size());
            }
            String newIndexData = String.join("\n", lines);
            saveIndexData(appId, newIndexData);
            log.debug("L0 索引追加成功，appId={}, chatHistoryId={}", appId, chatHistoryId);
        } catch (Exception e) {
            log.warn("L0 索引追加失败，appId={}: {}", appId, e.getMessage());
        }
    }

    private void saveIndexData(Long appId, String indexData) {
        MemIndexSnapshot existing = this.getOne(
                QueryWrapper.create().eq(MemIndexSnapshot::getAppId, appId)
        );
        if (existing == null) {
            MemIndexSnapshot snapshot = MemIndexSnapshot.builder()
                    .appId(appId)
                    .indexData(indexData)
                    .editTime(LocalDateTime.now())
                    .build();
            this.save(snapshot);
        } else {
            MemIndexSnapshot update = new MemIndexSnapshot();
            update.setId(existing.getId());
            update.setIndexData(indexData);
            update.setEditTime(LocalDateTime.now());
            this.updateById(update);
        }
        String redisKey = REDIS_KEY_PREFIX + appId;
        try {
            stringRedisTemplate.opsForValue().set(redisKey, indexData, REDIS_TTL);
        } catch (Exception e) {
            log.warn("L0 索引写 Redis 失败，appId={}: {}", appId, e.getMessage());
        }
    }
}
