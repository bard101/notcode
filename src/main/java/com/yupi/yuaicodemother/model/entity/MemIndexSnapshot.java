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
 * L1 层：记忆索引快照实体
 * 存储该 App 下所有 L2 设计决策的 title 索引字符串，Redis 缓存加速读取
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("mem_index_snapshot")
public class MemIndexSnapshot implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    @Column("appId")
    private Long appId;

    /**
     * 压缩后的索引字符串，格式: {topicId}:{title};{topicId}:{title}
     * 示例: "102:暗色系深蓝配色;105:固定顶部导航栏"
     */
    @Column("index_data")
    private String indexData;

    @Column("editTime")
    private LocalDateTime editTime;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;

    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}
