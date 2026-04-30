package com.yupi.yuaicodemother.ai;

import com.yupi.yuaicodemother.ai.model.HtmlCodeResult;
import com.yupi.yuaicodemother.ai.model.MultiFileCodeResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import reactor.core.publisher.Flux;

public interface AiCodeGeneratorService {

    /**
     * 生成 HTML 代码（非流式）
     *
     * @param userMessage          用户提示词
     * @param requirementSummary   需求摘要（注入到 System Prompt {{requirementSummary}}）
     * @param warmMemory           项目温数据（编码偏好、架构约定，注入 {{warmMemory}}）
     */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    HtmlCodeResult generateHtmlCode(
            @UserMessage String userMessage,
            @V("requirementSummary") String requirementSummary,
            @V("warmMemory") String warmMemory
    );

    /**
     * 生成多文件代码（非流式）
     *
     * @param userMessage          用户提示词
     * @param requirementSummary   需求摘要
     * @param warmMemory           项目温数据
     */
    @SystemMessage(fromResource = "prompt/codegen-multi-file-system-prompt.txt")
    MultiFileCodeResult generateMultiFileCode(
            @UserMessage String userMessage,
            @V("requirementSummary") String requirementSummary,
            @V("warmMemory") String warmMemory
    );

    /**
     * 生成 HTML 代码（流式）
     *
     * @param userMessage          用户提示词
     * @param requirementSummary   需求摘要
     * @param warmMemory           项目温数据
     */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    Flux<String> generateHtmlCodeStream(
            @UserMessage String userMessage,
            @V("requirementSummary") String requirementSummary,
            @V("warmMemory") String warmMemory
    );

    /**
     * 生成多文件代码（流式）
     *
     * @param userMessage          用户提示词
     * @param requirementSummary   需求摘要
     * @param warmMemory           项目温数据
     */
    @SystemMessage(fromResource = "prompt/codegen-multi-file-system-prompt.txt")
    Flux<String> generateMultiFileCodeStream(
            @UserMessage String userMessage,
            @V("requirementSummary") String requirementSummary,
            @V("warmMemory") String warmMemory
    );
}
