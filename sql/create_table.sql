/*
 Navicat Premium Data Transfer

 Source Server         : bard
 Source Server Type    : MySQL
 Source Server Version : 80039
 Source Host           : localhost:3306
 Source Schema         : notcode

 Target Server Type    : MySQL
 Target Server Version : 80039
 File Encoding         : 65001

 Date: 18/04/2026 19:50:17
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for app
-- ----------------------------
DROP TABLE IF EXISTS `app`;
CREATE TABLE `app`  (
                        `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
                        `appName` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '应用名称',
                        `cover` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '应用封面',
                        `initPrompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '应用初始化的 prompt',
                        `codeGenType` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '代码生成类型（枚举）',
                        `deployKey` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '部署标识',
                        `deployedTime` datetime NULL DEFAULT NULL COMMENT '部署时间',
                        `priority` int NOT NULL DEFAULT 0 COMMENT '优先级',
                        `userId` bigint NOT NULL COMMENT '创建用户id',
                        `editTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
                        `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                        `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                        `isDelete` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除',
                        `requirementSummary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '需求摘要（当前网站状态描述，由AI生成）',
                        `designPreferences` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '设计偏好（结构化JSON，由AI提取）',
                        PRIMARY KEY (`id`) USING BTREE,
                        UNIQUE INDEX `uk_deployKey`(`deployKey` ASC) USING BTREE,
                        INDEX `idx_appName`(`appName` ASC) USING BTREE,
                        INDEX `idx_userId`(`userId` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 402785833880760321 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '应用' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for chat_history
-- ----------------------------
DROP TABLE IF EXISTS `chat_history`;
CREATE TABLE `chat_history`  (
                                 `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
                                 `message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息',
                                 `messageType` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'user/ai',
                                 `appId` bigint NOT NULL COMMENT '应用id',
                                 `userId` bigint NOT NULL COMMENT '创建用户id',
                                 `request_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '请求ID，关联同一次请求的user和ai消息',
                                 `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                 `isDelete` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除',
                                 PRIMARY KEY (`id`) USING BTREE,
                                 INDEX `idx_appId`(`appId` ASC) USING BTREE,
                                 INDEX `idx_createTime`(`createTime` ASC) USING BTREE,
                                 INDEX `idx_appId_createTime`(`appId` ASC, `createTime` ASC) USING BTREE,
                                 FULLTEXT INDEX `ft_message`(`message`) WITH PARSER `ngram`,
                                 INDEX `idx_request_id`(`request_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 402787719220408321 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '对话历史' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for code_snapshot
-- ----------------------------
DROP TABLE IF EXISTS `code_snapshot`;
CREATE TABLE `code_snapshot`  (
                                  `id` bigint NOT NULL AUTO_INCREMENT,
                                  `appId` bigint NOT NULL COMMENT '所属应用ID',
                                  `version` int NOT NULL DEFAULT 1 COMMENT '版本号（自增）',
                                  `codeGenType` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '生成类型（html/multi_file）',
                                  `codePath` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '代码目录绝对路径',
                                  `triggerMessage` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '触发本次生成的用户消息',
                                  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  PRIMARY KEY (`id`) USING BTREE,
                                  INDEX `idx_appId_version`(`appId` ASC, `version` ASC) USING BTREE,
                                  INDEX `idx_appId_createTime`(`appId` ASC, `createTime` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 402786093873082370 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '代码版本快照' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for mem_index_snapshot
-- ----------------------------
DROP TABLE IF EXISTS `mem_index_snapshot`;
CREATE TABLE `mem_index_snapshot`  (
                                       `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键id',
                                       `appId` bigint NOT NULL COMMENT '应用id',
                                       `index_data` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '压缩后的索引: id:title;id:title',
                                       `editTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                       `isDelete` tinyint NOT NULL DEFAULT 0,
                                       PRIMARY KEY (`id`) USING BTREE,
                                       UNIQUE INDEX `uk_appId`(`appId` ASC) USING BTREE,
                                       INDEX `idx_appId`(`appId` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 402761365384921089 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '应用记忆索引快照' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for mem_topic
-- ----------------------------
DROP TABLE IF EXISTS `mem_topic`;
CREATE TABLE `mem_topic`  (
                              `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记忆ID',
                              `appId` bigint NOT NULL COMMENT '应用id',
                              `userId` bigint NOT NULL COMMENT '用户id',
                              `request_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '触发此决策写入的请求ID',
                              `topic_category` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '决策类别枚举',
                              `topic_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '内容MD5（变更检测用）',
                              `title` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标题摘要（12字内，填充L1索引）',
                              `content_md` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'AI精炼的设计决策Markdown',
                              `priority` int NOT NULL DEFAULT 0,
                              `editTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                              `isDelete` tinyint NOT NULL DEFAULT 0,
                              PRIMARY KEY (`id`) USING BTREE,
                              UNIQUE INDEX `uk_app_category`(`appId` ASC, `topic_category` ASC) USING BTREE,
                              INDEX `idx_app_user`(`appId` ASC, `userId` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 402761364982267905 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '应用记忆内容库' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
                         `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
                         `userAccount` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '账号',
                         `userPassword` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码',
                         `userName` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户昵称',
                         `userAvatar` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户头像',
                         `userProfile` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户简介',
                         `userRole` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'user' COMMENT '用户角色：user/admin',
                         `editTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
                         `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                         `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                         `isDelete` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除',
                         PRIMARY KEY (`id`) USING BTREE,
                         UNIQUE INDEX `uk_userAccount`(`userAccount` ASC) USING BTREE,
                         INDEX `idx_userName`(`userName` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 402374648043737089 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
