package com.yupi.yuaicodemother.service;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yupi.yuaicodemother.mapper.MemTopicMapper;
import com.yupi.yuaicodemother.model.dto.TopicDTO;
import com.yupi.yuaicodemother.model.entity.MemTopic;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * L2 精炼设计决策服务
 * - upsertTopics: 将 AI 反思结果写入 mem_topic，MD5 检测变更避免无效覆盖
 * - buildPromptContext: 以 Markdown 格式聚合该 App 所有设计决策，注入 Prompt
 */
@Service
@Slf4j
public class MemoryTopicService extends ServiceImpl<MemTopicMapper, MemTopic> {

    /**
     * 新增或更新设计决策列表
     *
     * @param appId     应用 ID
     * @param userId    用户 ID
     * @param requestId 触发此次反思的请求ID（用于追溯）
     * @param topics    AI 反思输出的决策列表
     */
    public void upsertTopics(Long appId, Long userId, String requestId, List<TopicDTO> topics) {
        if (appId == null || topics == null || topics.isEmpty()) {
            return;
        }
        for (TopicDTO dto : topics) {
            if (StrUtil.isBlank(dto.getTopicCategory()) || StrUtil.isBlank(dto.getContentMd())) {
                continue;
            }
            // 跳过低质量猜测条目（priority < 1 表示 AI 认为该类别信息不足，不应写入）
            if (dto.getPriority() != null && dto.getPriority() < 1) {
                log.debug("跳过低优先级猜测条目(priority={}), category={}", dto.getPriority(), dto.getTopicCategory());
                continue;
            }
            String newHash = DigestUtil.md5Hex(dto.getContentMd());

            // 查询是否已存在该 (appId, topicCategory) 条目
            MemTopic existing = this.getOne(
                    QueryWrapper.create()
                            .eq(MemTopic::getAppId, appId)
                            .eq(MemTopic::getTopicCategory, dto.getTopicCategory())
            );

            if (existing == null) {
                // 新增
                MemTopic newTopic = MemTopic.builder()
                        .appId(appId)
                        .userId(userId)
                        .requestId(requestId)
                        .topicCategory(dto.getTopicCategory())
                        .topicHash(newHash)
                        .title(dto.getTitle())
                        .contentMd(dto.getContentMd())
                        .priority(dto.getPriority() != null ? dto.getPriority() : 0)
                        .editTime(LocalDateTime.now())
                        .build();
                this.save(newTopic);
                log.debug("新增设计决策，appId={}, category={}", appId, dto.getTopicCategory());
            } else if (!newHash.equals(existing.getTopicHash())) {
                // 内容有变化才更新（MD5 防无效写入）
                MemTopic update = new MemTopic();
                update.setId(existing.getId());
                update.setRequestId(requestId);
                update.setTopicHash(newHash);
                update.setTitle(dto.getTitle());
                update.setContentMd(dto.getContentMd());
                update.setEditTime(LocalDateTime.now());
                if (dto.getPriority() != null) {
                    update.setPriority(dto.getPriority());
                }
                this.updateById(update);
                log.debug("更新设计决策，appId={}, category={}", appId, dto.getTopicCategory());
            } else {
                log.debug("设计决策无变化跳过，appId={}, category={}", appId, dto.getTopicCategory());
            }
        }
    }

    /**
     * 构建注入 Prompt 的 L2 上下文字符串
     * 格式：Markdown 无序列表，按 priority 降序
     *
     * @param appId 应用 ID
     * @return Markdown 字符串，无数据时返回空字符串
     */
    public String buildPromptContext(Long appId) {
        if (appId == null || appId <= 0) {
            return "";
        }
        List<MemTopic> topics = this.list(
                QueryWrapper.create()
                        .eq(MemTopic::getAppId, appId)
                        .orderBy(MemTopic::getPriority, false)
        );
        if (topics == null || topics.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("## 用户设计偏好（请遵循）\n\n");
        for (MemTopic topic : topics) {
            if (StrUtil.isNotBlank(topic.getTitle())) {
                sb.append("### ").append(topic.getTitle()).append("\n");
            }
            if (StrUtil.isNotBlank(topic.getContentMd())) {
                sb.append(topic.getContentMd()).append("\n\n");
            }
        }
        return sb.toString().trim();
    }
}
