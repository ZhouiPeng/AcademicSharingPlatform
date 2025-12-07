# Data Sync Service — 项目结构说明

本文档由自动扫描生成，目标是说明 `services/data-sync-service` 中的文件与目录含义，并给出保留/清理建议。

## 概览
- 位置：`services/data-sync-service`
- 语言/构建：Java 17 + Maven（`pom.xml`）
- 运行方式：Spring Boot 应用（入口 `com.academic.datasync.DataSyncApplication`）

## 项目架构
下面给出一个简洁的架构视图（ASCII 图），展示模块之间的关系和数据流：

```
	     +--------------------+
	     |  外部调用者 / 前端  |
	     +---------+----------+
		       |
		       v
	     +---------+----------+
	     |  DataSyncController |  <-- REST API（HTTP）
	     +---------+----------+
		       |
		       v
	     +---------+----------+
	     |    DataSyncService  |  <-- 业务逻辑（Service 层）
	     +---------+----------+
	      /      |       \
	     /       |        \
	    v        v         v
 +----------------+  |  +-----------------+   +--------------------+
 | FileServiceClient|  |  | AchievementSvc  |   | Other External API |
 +----------------+  |  +-----------------+   +--------------------+
		     |
		     v
	     +--------------------+
	     |  WebClient / HTTP  |  <-- 在 `client` 中实现，使用 `WebClient` 或 RestTemplate
	     +--------------------+
```

组件说明：
- Controller：`controller` 包，负责接收 HTTP 请求并返回响应。
- Service：`service` 包，包含 `DataSyncService` 接口与实现 `DataSyncServiceImpl`，实现核心业务流程（下载、处理、上传）。
- Client：`client` 包，封装与其它微服务（如文件服务、achievement 服务）或第三方 API 的 HTTP 调用。
- Config：`config` 包（例如 `WebClientConfig`），放置共享的客户端/连接/序列化配置。
- Swagger：通过 `springdoc` 提供 API 文档，`SwaggerController` 提供自定义 UI，`SwaggerUrlPrinter` 在启动时打印访问地址。
- Tests：`src/test/java` 包含若干集成/单元测试，用于验证上传/下载/预览等功能。
- Packaging：`pom.xml`（Maven）与 `Dockerfile` 用于构建与容器化部署。

架构说明（要点）：
- 解耦：Controller 与 Service 分离，Service 通过 Client 与外部服务通信，便于测试与替换依赖。
- 可扩展：新增外部服务集成只需添加或扩展 `client`，Service 层聚合调用逻辑。
- 测试友好：将样本响应/数据放入 `src/test/resources` 可让测试稳定运行，不依赖外网。


## 顶层文件
- `pom.xml`：Maven 构建描述文件，定义依赖和插件。保留。
- `Dockerfile`：容器镜像构建脚本（可选用于部署），保留（若不使用 Docker 可删除）。

## 主要源码
- `src/main/java/com/academic/datasync/DataSyncApplication.java`：应用主类（Spring Boot 启动入口）。保留。
- `src/main/java/com/academic/datasync/controller/DataSyncController.java`：REST 控制器，暴露业务 API。保留。
- `src/main/java/com/academic/datasync/service/*`：服务接口与实现，包含业务逻辑。保留。
- `src/main/java/com/academic/datasync/client/*`：调用其他服务的客户端（HTTP/webclient 等）。保留。
- `src/main/java/com/academic/datasync/config/WebClientConfig.java`：配置类（如 WebClient）。保留。

### Swagger 相关（辅助）
- `src/main/java/com/academic/datasync/SwaggerController.java`：提供一个嵌入式的 Swagger UI HTML 路由（`/swagger-ui/index.html`）。如果你使用 `springdoc`，这个控制器作为自定义 UI 页面存在价值，建议保留或根据需要移除。
- `src/main/java/com/academic/datasync/SwaggerUrlPrinter.java`：应用启动时打印 Swagger URL 的监听器，仅用于开发友好提示，属于辅助组件，可保留或移除（不会影响核心逻辑）。

## 测试代码
- `src/test/java/com/academic/datasync/*`：JUnit 测试类（`DataSyncSmokeTest`、`UploadFromUrlTest` 等）。建议保留，用于回归测试与 CI。

## 临时 / 需要清理的文件
- `src/main/java/com/academic/datasync/.LCKDataSyncApplication.java~`：编辑器或 IDE 生成的备份/锁文件（`*~`），不是源码的一部分，建议删除并把类似模式加入 `.gitignore`。
- `src/main/java/com/example/datasync/DataSyncApplication.java`：此文件为占位/注释形式（包含说明“Disabled duplicate main class”）。建议删除或移动到 `docs/`，以免与主包产生混淆或造成重复的 main 类。

## 根目录下的 `*.txt` 调试/示例文件
在项目根目录下存在多份文本文件（HTTP 响应、调试输出或示例数据）：

- 例如：`upload_resp.txt`, `upload_resp2.txt`, `upload_output.txt` ~ `upload_output6.txt`, `upload_output_debug.txt`, `upload_dummy_resp.txt`, `download_dummy_resp.txt`, `download_transformer_resp.txt`, `download2.txt`, `dummy_dl.txt`, `obs_check.txt`, `arxiv_head.txt`, `arxiv_headers.txt`。

这些文件在源码中没有检索到引用（仅为调试或示例输出）。建议：
- 若只是临时调试产物：删除并在仓库根添加 `.gitignore` 规则（见下）。
- 若为测试用样本：移动到 `src/test/resources/`，并在测试中通过类路径加载，这样更清晰且易于版本控制。

## 推荐的 `.gitignore` 条目
```
/target/
*.log
*~
/*.txt    # 如果你不希望保留所有根目录 txt 的话（谨慎）。
/.idea/
*.iml
```

建议仅在确认那些 `*.txt` 确实为调试文件时才使用 `/*.txt`，否则改为列出特定文件名。

## 构建与运行（快速命令）
Windows (`cmd.exe`) 下：
```bat
cd services/data-sync-service
mvn -U -DskipTests=false clean package
mvn spring-boot:run
```

使用 Docker（若 `Dockerfile` 有效）：
```bat
cd services/data-sync-service
docker build -t data-sync-service:local .
docker run --rm -p 8080:8080 data-sync-service:local
```

## 总结建议（操作清单）
- 保留：`pom.xml`、`src/main/java`（实际源码）、`src/main/resources/application.yml`、`src/test/java`、`Dockerfile`（如用 Docker）。
- 移除或归档：所有根目录下看起来是调试输出的 `*.txt` 文件，和编辑器备份文件 `*~`。
- 清理重复/占位类：`src/main/java/com/example/datasync/DataSyncApplication.java` 可删除或移到 `docs/`。
- 可选：将示例响应文件移动到 `src/test/resources/` 并在测试中引用。

如需，我可以：
- （A）自动添加/更新 `.gitignore` 并删除确认的不需要文件；
- （B）把部分 `*.txt` 移动到 `src/test/resources/`；
- （C）提交这些更改到当前分支（需要你的确认）。

---
生成于：2025-12-07
