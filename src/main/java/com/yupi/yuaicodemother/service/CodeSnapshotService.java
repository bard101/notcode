package com.yupi.yuaicodemother.service;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yupi.yuaicodemother.mapper.CodeSnapshotMapper;
import com.yupi.yuaicodemother.model.entity.CodeSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 代码版本快照服务
 */
@Service
@Slf4j
public class CodeSnapshotService extends ServiceImpl<CodeSnapshotMapper, CodeSnapshot> {

    /**
     * 异步保存版本快照
     *
     * @param appId          应用 ID
     * @param codeGenType    生成类型
     * @param codePath       代码目录路径
     * @param triggerMessage 触发本次生成的用户消息
     */
    public void saveSnapshotAsync(Long appId, String codeGenType, String codePath, String triggerMessage) {
        if (appId == null || StrUtil.isBlank(codePath)) {
            return;
        }
        // 检查目录是否实际存在，不存在则不记录
        if (!new File(codePath).exists()) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                // 获取该 appId 的最新版本号
                List<CodeSnapshot> latest = this.list(
                        QueryWrapper.create()
                                .eq(CodeSnapshot::getAppId, appId)
                                .orderBy(CodeSnapshot::getVersion, false)
                                .limit(1)
                );
                int nextVersion = latest.isEmpty() ? 1 : latest.get(0).getVersion() + 1;

                CodeSnapshot snapshot = CodeSnapshot.builder()
                        .appId(appId)
                        .version(nextVersion)
                        .codeGenType(codeGenType)
                        .codePath(codePath)
                        .triggerMessage(triggerMessage)
                        .createTime(LocalDateTime.now())
                        .build();
                this.save(snapshot);
                log.debug("版本快照已保存，appId={}, version={}", appId, nextVersion);
            } catch (Exception e) {
                log.warn("保存版本快照失败，appId={}: {}", appId, e.getMessage());
            }
        });
    }

    /**
     * 查询应用的历史版本列表（按版本降序）
     *
     * @param appId 应用 ID
     * @return 快照列表
     */
    public List<CodeSnapshot> listByAppId(Long appId) {
        return this.list(
                QueryWrapper.create()
                        .eq(CodeSnapshot::getAppId, appId)
                        .orderBy(CodeSnapshot::getVersion, false)
        );
    }
}
