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
 * 代码版本快照实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("code_snapshot")
public class CodeSnapshot implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    @Column("appId")
    private Long appId;

    /**
     * 版本号（同一 appId 自增）
     */
    private Integer version;

    /**
     * 代码生成类型（html / multi_file）
     */
    @Column("codeGenType")
    private String codeGenType;

    /**
     * 代码目录绝对路径
     */
    @Column("codePath")
    private String codePath;

    /**
     * 触发本次生成的用户消息
     */
    @Column("triggerMessage")
    private String triggerMessage;

    @Column("createTime")
    private LocalDateTime createTime;
}
