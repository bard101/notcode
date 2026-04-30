package com.yupi.yuaicodemother.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 反思输出的单条设计决策 DTO
 * 由反思 Prompt 输出 JSON 数组，再反序列化为此 DTO 列表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopicDTO {

    /**
     * 决策类别枚举，8 类之一：
     * color_scheme / layout_style / typography / interactivity /
     * content_tone / component_preference / navigation / responsiveness
     */
    private String topicCategory;

    /** 12 字以内的标题 */
    private String title;

    /** 设计决策 Markdown 正文 */
    private String contentMd;

    /** 优先级，默认 0 */
    private Integer priority;
}
