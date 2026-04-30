package com.yupi.yuaicodemother.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * L2 层：精炼设计决策实体
 * 每个 (appId, topic_category) 唯一，AI 反思后写入/覆盖
 * topic_category 枚举值（8 类固定）：
 *   color_scheme / layout_style / typography / interactivity /
 *   content_tone / component_preference / navigation / responsiveness
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("mem_topic")
public class MemTopic implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    @Column("appId")
    private Long appId;

    @Column("userId")
    private Long userId;

    /** 触发此决策写入的请求ID，用于追溯 */
    @Column("request_id")
    private String requestId;

    /** 决策类别枚举，见类注释 */
    @Column("topic_category")
    private String topicCategory;

    /** content_md 的 MD5，用于检测是否真正变化，避免无效更新 */
    @Column("topic_hash")
    private String topicHash;

    /** 12 字以内的标题，供 L1 索引压缩使用 */
    @Column("title")
    private String title;

    /** AI 精炼后的设计决策 Markdown 内容 */
    @Column("content_md")
    private String contentMd;

    /** 优先级（越大越优先展示给 AI），默认 0 */
    @Column("priority")
    private Integer priority;

    @Column("editTime")
    private LocalDateTime editTime;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;

    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}
