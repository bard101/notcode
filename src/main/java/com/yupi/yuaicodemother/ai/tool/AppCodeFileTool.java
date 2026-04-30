package com.yupi.yuaicodemother.ai.tool;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * 代码文件操作工具（per-appId POJO，非 Spring Bean）
 *
 * <p>注册到 LangChain4j AiServices，AI 可通过工具调用精确读写网站文件，
 * 无需每次全量重写代码，大幅降低 Token 消耗并提升修改精度。
 *
 * <p>适用于：修改按钮颜色、调整字体、新增模块等局部修改场景。
 */
@Slf4j
public class AppCodeFileTool {

    private final long appId;
    private final String codeOutputDir;

    public AppCodeFileTool(long appId, String codeGenType, String codeOutputRootDir) {
        this.appId = appId;
        this.codeOutputDir = codeOutputRootDir + File.separator + codeGenType + "_" + appId;
    }

    @Tool("获取当前网站的完整代码内容。修改已有网站前必须先调用此方法了解代码结构，首次生成则无需调用。")
    public String getCurrentCode() {
        File dir = new File(codeOutputDir);
        if (!dir.exists() || !dir.isDirectory()) {
            return "当前应用尚无代码，请直接生成完整网站代码。";
        }
        StringBuilder result = new StringBuilder();
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            return "代码目录为空，请直接生成完整网站代码。";
        }
        for (File file : files) {
            if (file.isFile()) {
                result.append("=== ").append(file.getName()).append(" ===\n");
                result.append(FileUtil.readString(file, StandardCharsets.UTF_8));
                result.append("\n\n");
            }
        }
        return result.toString();
    }

    @Tool("更新网站的 index.html 文件。传入完整的 HTML 内容，将覆盖原文件。")
    public String updateHtmlFile(@P("content") String content) {
        return writeFile("index.html", content);
    }

    @Tool("更新网站的 style.css 文件（仅适用于多文件类型应用）。传入完整的 CSS 内容，将覆盖原文件。")
    public String updateCssFile(@P("content") String content) {
        return writeFile("style.css", content);
    }

    @Tool("更新网站的 script.js 文件（仅适用于多文件类型应用）。传入完整的 JS 内容，将覆盖原文件。")
    public String updateJsFile(@P("content") String content) {
        return writeFile("script.js", content);
    }

    private String writeFile(String filename, String content) {
        if (StrUtil.isBlank(content)) {
            return "错误：文件内容不能为空";
        }
        try {
            File dir = new File(codeOutputDir);
            if (!dir.exists()) {
                FileUtil.mkdir(dir);
            }
            String filePath = codeOutputDir + File.separator + filename;
            FileUtil.writeString(content, filePath, StandardCharsets.UTF_8);
            log.info("工具调用：已更新文件 {}，appId={}", filename, appId);
            return "success";
        } catch (Exception e) {
            log.error("工具调用写文件失败，filename={}，appId={}: {}", filename, appId, e.getMessage());
            return "错误：" + e.getMessage();
        }
    }
}
