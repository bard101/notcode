# NotCode

NotCode 是一个 AI 驱动的网页应用生成与管理平台，支持通过自然语言生成单页网页或多文件静态站点，并提供对话式继续修改、预览、部署和后台管理能力。

本仓库是课程练习中的二次开发版本，当前内容以本人实际修改后的可运行工程为准，已移除与当前仓库无关的课程宣传信息。

## 项目概览

- 后端基于 Spring Boot 3、MyBatis Flex、Redis、LangChain4j
- 前端基于 Vue 3、Vite、Ant Design Vue
- 支持 HTML 单文件生成和多文件网站生成
- 支持流式返回生成过程、应用预览、静态部署与后台管理
- 当前版本加入了对话记忆、代码快照、工具调用与更细的预览/部署路径处理

## 主要功能

1. 智能生成代码
	用户输入需求后，系统调用大模型生成网页代码，并以流式方式返回结果。

2. 对话式迭代修改
	在已有应用上继续提需求，可以基于当前代码做局部修改，而不是每次全量重建。

3. 预览与部署
	生成后的代码可直接预览，并可按部署标识访问发布后的静态内容。

4. 管理后台
	提供用户管理、应用管理与基础运营能力。

## 目录结构

```text
.
├─src/                          后端源码
├─sql/                          数据库脚本
├─yu-ai-code-mother-frontend/   前端工程
├─tmp/                          本地生成与部署临时目录
└─src/main/resources/prompt/    提示词模板
```

## 本地启动

### 1. 后端

需要准备：

- JDK 21
- Maven Wrapper
- MySQL 8
- Redis

推荐先配置本地环境变量或本地覆盖配置中的以下参数：

- DB_USERNAME
- DB_PASSWORD
- DEEPSEEK_API_KEY
- PEXELS_API_KEY
- FIRECRAWL_API_KEY

启动命令：

```bash
./mvnw spring-boot:run
```

Windows：

```powershell
.\mvnw.cmd spring-boot:run
```

### 2. 前端

```bash
cd yu-ai-code-mother-frontend
npm install
npm run dev
```

## 构建验证

后端编译：

```powershell
.\mvnw.cmd -DskipTests compile
```

前端构建：

```bash
cd yu-ai-code-mother-frontend
npm run build
```

## 说明

- 本仓库保留课程练习与二次开发的工程结构
- 本地敏感配置应放在未跟踪文件或环境变量中
- tmp、target、dump.rdb 等运行产物不应提交到仓库
