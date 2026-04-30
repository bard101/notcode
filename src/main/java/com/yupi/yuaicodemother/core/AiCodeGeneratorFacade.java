package com.yupi.yuaicodemother.core;

import com.yupi.yuaicodemother.ai.AiCodeGeneratorService;
import com.yupi.yuaicodemother.ai.AiCodeGeneratorServiceFactory;
import com.yupi.yuaicodemother.ai.model.HtmlCodeResult;
import com.yupi.yuaicodemother.ai.model.MultiFileCodeResult;
import com.yupi.yuaicodemother.constant.AppConstant;
import com.yupi.yuaicodemother.core.parser.CodeParserExecutor;
import com.yupi.yuaicodemother.core.saver.CodeFileSaverExecutor;
import com.yupi.yuaicodemother.exception.BusinessException;
import com.yupi.yuaicodemother.exception.ErrorCode;
import com.yupi.yuaicodemother.model.enums.CodeGenTypeEnum;
import com.yupi.yuaicodemother.service.CodeSnapshotService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * AI 代码生成门面类，组合代码生成、保存、版本快照、记忆更新功能
 */
@Service
@Slf4j
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    @Resource
    private CodeSnapshotService codeSnapshotService;

    /**
     * 统一入口：根据类型生成并保存代码（非流式）
     *
     * @param userMessage        用户提示词
     * @param codeGenTypeEnum    生成类型
     * @param appId              应用 ID
     * @param userId             用户 ID
     * @param requirementSummary 需求摘要（注入 System Prompt）
     * @param warmMemory         项目温数据（编码偏好 & 架构约定，注入 System Prompt）
     * @return 保存的目录
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId, Long userId,
                                    String requirementSummary, String warmMemory) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成类型不能为空");
        }
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(userMessage, requirementSummary, warmMemory);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(userMessage, requirementSummary, warmMemory);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的生成类型：" + codeGenTypeEnum.getValue());
        };
    }

    /**
     * 统一入口：根据类型生成并保存代码（流式）
     *
     * @param userMessage        用户提示词
     * @param codeGenTypeEnum    生成类型
     * @param appId              应用 ID
     * @param userId             用户 ID
     * @param requirementSummary 需求摘要（注入 System Prompt）
     * @param warmMemory         项目温数据（编码偏好 & 架构约定，注入 System Prompt）
     * @return 流式字符串
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId, Long userId,
                                                  String requirementSummary, String warmMemory) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成类型不能为空");
        }
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage, requirementSummary, warmMemory);
                yield processCodeStream(codeStream, CodeGenTypeEnum.HTML, appId, userId, userMessage);
            }
            case MULTI_FILE -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage, requirementSummary, warmMemory);
                yield processCodeStream(codeStream, CodeGenTypeEnum.MULTI_FILE, appId, userId, userMessage);
            }
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的生成类型：" + codeGenTypeEnum.getValue());
        };
    }

    /**
     * 通用流式代码处理方法
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType, Long appId, Long userId, String userMessage) {
        StringBuilder codeBuilder = new StringBuilder();
        return codeStream.doOnNext(codeBuilder::append)
                .doOnComplete(() -> {
                    try {
                        String completeCode = codeBuilder.toString();
                        // 解析代码块（工具调用场景下可能无代码块，解析结果字段为空）
                        Object parsedResult = CodeParserExecutor.executeParser(completeCode, codeGenType);
                        // 保存文件（若字段为空则跳过，保留工具直接写入的文件）
                        CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType, appId);

                        // 计算代码目录路径，无论是代码块保存还是工具保存，目录相同
                        String codePath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator
                                + codeGenType.getValue() + "_" + appId;
                        // 异步保存版本快照
                        codeSnapshotService.saveSnapshotAsync(appId, codeGenType.getValue(), codePath, userMessage);
                        // 记忆更新事件由 AppServiceImpl 在 AI 消息落库后发布，此处不再发布

                        log.info("流式生成完成，appId={}, codeGenType={}", appId, codeGenType.getValue());
                    } catch (Exception e) {
                        log.error("流式生成后处理失败，appId={}: {}", appId, e.getMessage());
                    }
                });
    }
}

