package com.yupi.yuaicodemother.service;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.row.Db;
import com.mybatisflex.core.row.Row;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * L2 深度历史检索服务（全文检索）
 * 依赖 chat_history 表上的 FULLTEXT INDEX ft_message (message, title) WITH PARSER ngram
 * 支持中文分词全文检索，返回匹配对话记录的摘要
 */
@Service
@Slf4j
public class MemoryDeepSearchService {

    /**
     * 全文检索对话历史，返回匹配对话片段
     *
     * @param appId   应用 ID
     * @param keyword 检索关键词
     * @return 匹配的对话片段，无结果时返回空字符串
     */
    public String search(Long appId, String keyword) {
        if (appId == null || appId <= 0 || StrUtil.isBlank(keyword)) {
            return "";
        }
        try {
            // 全文检索匹配记录（BOOLEAN MODE，支持中文 ngram，同时搜索 message 和 title）
            String searchSql = """
                    SELECT id, title, ai_summary, createTime FROM chat_history
                    WHERE appId = ?
                      AND isDelete = 0
                      AND MATCH(message, title) AGAINST(? IN BOOLEAN MODE)
                    ORDER BY createTime DESC
                    LIMIT 10
                    """;
            List<Row> matchedRows = Db.selectListBySql(searchSql, appId, keyword);
            if (matchedRows == null || matchedRows.isEmpty()) {
                return "";
            }

            List<String> fragments = new ArrayList<>();
            for (Row row : matchedRows) {
                String title = row.getString("title");
                String aiSummary = row.getString("ai_summary");
                StringBuilder fragment = new StringBuilder("--- 相关对话 ---\n");
                fragment.append("用户提问：").append(title).append("\n");
                if (StrUtil.isNotBlank(aiSummary)) {
                    fragment.append("AI摘要：").append(aiSummary).append("\n");
                }
                fragments.add(fragment.toString());
            }

            return String.join("\n", fragments);
        } catch (Exception e) {
            log.warn("L2 全文检索失败，appId={}, keyword={}: {}", appId, keyword, e.getMessage());
            return "";
        }
    }
}
