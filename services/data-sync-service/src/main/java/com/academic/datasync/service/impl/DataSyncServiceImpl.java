// 包声明：定义当前类所在的包，表示这个实现属于 data-sync-service 的服务实现包
package com.academic.datasync.service.impl; // 定义包名

// 引入需要的类与接口，下面每行都附带中文注释以说明用途
import java.io.StringReader; // 用于把字符串包装为 Reader 以便 XML 解析
import java.net.URLEncoder; // 用于对 arXiv 查询字符串进行 URL 编码
import java.nio.charset.StandardCharsets; // 提供标准字符集常量（UTF-8）
import java.time.Duration; // 表示时间段，用于 WebClient 的超时
import java.util.HashMap; // 提供 HashMap 实现
import java.util.Iterator; // 用于遍历集合的迭代器
import java.util.Map; // Map 接口，用于构建上报负载
import java.util.regex.Matcher; // 正则匹配器
import java.util.regex.Pattern; // 正则模式

import javax.xml.parsers.DocumentBuilder; // DOM 解析器的构建器
import javax.xml.parsers.DocumentBuilderFactory; // DOM DocumentBuilder 的工厂

import org.slf4j.Logger; // 日志接口
import org.slf4j.LoggerFactory; // 日志工厂，用于创建 Logger
import org.springframework.stereotype.Service; // 标注当前类为 Spring 的 Service 组件
import org.springframework.web.reactive.function.client.WebClient; // 非阻塞的 HTTP 客户端
import org.w3c.dom.Document; // DOM Document 表示解析后的 XML 文档
import org.w3c.dom.Element; // DOM Element 表示 XML 元素
import org.w3c.dom.NodeList; // DOM NodeList 表示节点列表
import org.xml.sax.InputSource; // 将字符串包装为 InputSource 供解析器使用

import com.academic.datasync.client.AchievementServiceClient; // 成就服务客户端接口（注入）
import com.academic.datasync.client.FileServiceClient; // 文件服务客户端接口（注入）
import com.academic.datasync.service.DataSyncService; // DataSync 服务接口
import com.fasterxml.jackson.databind.JsonNode; // Jackson 的 JsonNode，用于 JSON 树解析
import com.fasterxml.jackson.databind.ObjectMapper; // Jackson 的 ObjectMapper，用于 JSON 序列化/反序列化

@Service // 声明这是一个 Spring 管理的服务组件
public class DataSyncServiceImpl implements DataSyncService { // 实现 DataSyncService 接口

    // 日志记录器，用于输出运行时信息
    private static final Logger log = LoggerFactory.getLogger(DataSyncServiceImpl.class); // 获取当前类的 Logger

    // WebClient 实例用于访问 OpenAlex API（元数据）
    private final WebClient webClient; // 用于调用 OpenAlex 的客户端
    // arXiv 的 WebClient 用于获取 Atom feed
    private final WebClient arxivClient; // 用于调用 arXiv API 的客户端
    // JSON 解析器实例
    private final ObjectMapper objectMapper = new ObjectMapper(); // Jackson 的 ObjectMapper
    // 成就服务客户端（注入）
    private final AchievementServiceClient achievementClient; // 注入用于上报成就的客户端
    // 文件服务客户端（注入）
    private final FileServiceClient fileServiceClient; // 注入用于上传文件的客户端

    // 构造函数：通过 Spring 注入 WebClient.Builder 和客户端实现
    public DataSyncServiceImpl(WebClient.Builder builder,
            AchievementServiceClient achievementClient,
            FileServiceClient fileServiceClient) {
        // 为 WebClient 配置 exchange strategies（增加内存缓存上限，避免大响应导致 OOM）
        org.springframework.web.reactive.function.client.ExchangeStrategies strategies
                = org.springframework.web.reactive.function.client.ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(5 * 1024 * 1024))
                        .build(); // 设置最大内存缓冲为 5 MiB
        this.webClient = builder.exchangeStrategies(strategies).baseUrl("https://api.openalex.org").build(); // OpenAlex 基础 URL
        this.arxivClient = org.springframework.web.reactive.function.client.WebClient.builder()
                .exchangeStrategies(strategies)
                .defaultHeader("User-Agent", "AcademicSharingPlatform/0.1 (github:AcademicSharingPlatform)")
                .defaultHeader("Accept", "application/atom+xml,application/xml;q=0.9,*/*;q=0.8")
                .baseUrl("https://export.arxiv.org")
                .build(); // arXiv API 的 WebClient，设置 UA 与 Accept header
        this.achievementClient = achievementClient; // 保存注入的成就客户端引用
        this.fileServiceClient = fileServiceClient; // 保存注入的文件客户端引用
    }

    @Override
    public void pullFromPublicDb() { // 从公共数据库（OpenAlex 和 arXiv）拉取并处理示例流程
        log.info("Starting pullFromPublicDb: fetching works from OpenAlex (demo limited)"); // 记录开始
        try {
            // 调用 OpenAlex /works 获取开放获取的论文列表（演示：每页 20 条）
            String body = webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/works")
                    .queryParam("filter", "is_oa:true")
                    .queryParam("per-page", "20")
                    .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(10)); // 阻塞等待最多 10 秒

            // 如果返回为空则记录并退出
            if (body == null) {
                log.warn("OpenAlex returned empty body"); // 提示空响应
                return; // 退出方法
            }

            // 将 JSON 字符串解析为树结构，并取出 results 数组
            JsonNode root = objectMapper.readTree(body); // 解析 JSON
            JsonNode results = root.get("results"); // 获取 results
            if (results == null || !results.isArray()) {
                log.warn("Unexpected OpenAlex response structure"); // 结构异常警告
                return; // 退出
            }

            // 遍历每个 work（论文记录）进行处理
            Iterator<JsonNode> it = results.elements(); // 获取迭代器
            while (it.hasNext()) {
                JsonNode work = it.next(); // 当前 work
                // 从 work 中读取标题、OpenAlex id、DOI
                String title = work.path("title").asText("untitled"); // 读取 title，默认 untitled
                String openalexId = work.path("id").asText(); // 读取 id
                String doi = work.path("doi").asText(null); // 读取 doi（可能为空）

                String pdfUrl = extractPdfUrl(work); // 尝试从 work 中提取 PDF URL

                // 生成回退的 fileId，如果没有找到真实文件则用 openalexId 或 doi
                String fallbackFileId = openalexId.isEmpty() ? (doi != null ? doi : "oa-" + System.currentTimeMillis()) : openalexId; // 回退 ID
                String finalFileId = fallbackFileId; // 最终使用的 fileId，可能被上传替换

                // 如果找到了 PDF URL，则尝试下载并通过 file-service 上传
                if (pdfUrl != null && !pdfUrl.isEmpty()) {
                    try {
                        log.info("Found PDF URL for work {}: {}", openalexId, pdfUrl); // 记录找到的 PDF URL

                        // 尝试用标题生成文件名，若缺失则使用 fallbackFileId
                        String rawTitle = title == null || title.isEmpty() ? finalFileId : title; // 选择原始名称
                        String filename = sanitizeFilename(rawTitle); // 清理为安全文件名

                        // 使用 fileServiceClient 从远程 URL 下载并上传（客户端实现负责下载/上传细节）
                        String uploadResp = fileServiceClient.uploadFromUrl("datasync", pdfUrl, filename); // 上传并获取响应
                        if (uploadResp != null) {
                            try {
                                JsonNode uploadRoot = objectMapper.readTree(uploadResp); // 解析上传响应 JSON
                                JsonNode data = uploadRoot.path("data"); // 取 data 字段
                                String uploadedFileId = data.path("fileId").asText(null); // 取 fileId
                                if (uploadedFileId != null && !uploadedFileId.isEmpty()) {
                                    finalFileId = uploadedFileId; // 使用上传返回的 fileId
                                    log.info("Uploaded PDFfor work {} to file-service, fileId={}", openalexId, finalFileId); // 记录成功
                                } else {
                                    log.warn("Upload response did not contain fileId for work {}: {}", openalexId, uploadResp); // 警告：响应无 fileId
                                }
                            } catch (Exception pe) {
                                log.warn("Failed to parse upload response for work {}: {}", openalexId, pe.getMessage()); // 上传响应解析失败
                            }
                        } else {
                            log.warn("File upload returned null for work {}", openalexId); // 上传返回 null
                        }
                    } catch (Exception de) {
                        log.warn("Failed to download or upload PDF for work {}: {}", openalexId, de.getMessage()); // 下载或上传失败
                    }
                } else {
                    log.info("No PDF URL found for work {}. Using fallback fileId {}", openalexId, finalFileId); // 未找到 PDF，使用回退 ID
                }

                // 构造成就上报的最小负载（示例）
                Map<String, Object> achPayload = new HashMap<>(); // 新建 Map
                achPayload.put("userId", "datasync"); // 填充 userId
                achPayload.put("title", title); // 填充标题
                achPayload.put("fileId", finalFileId); // 填充 fileId

                // 将负载序列化为 JSON 并（演示）打印，实际调用被跳过
                String jsonPayload = objectMapper.writeValueAsString(achPayload); // 转为 JSON 字符串
                log.info("[TEST MODE] Skipping achievement service call for work {} -> payload={}", openalexId, jsonPayload); // 测试模式打印
            }

            // OpenAlex 部分处理完成，接着尝试从 arXiv 拉取记录
            int arxivCount = 20; // arXiv 拉取数量（与 OpenAlex 对应）
            try {
                // 默认查询示例：按类别查询（可修改为更具体的查询）
                final String ARXIV_SEARCH_QUERY = "cat:cs.AI"; // 默认查询：CS.AI 类别
                String encoded = ARXIV_SEARCH_QUERY;//encodeArxivQuery(ARXIV_SEARCH_QUERY); // 对查询进行 URL 编码
                String path = "/api/query?search_query=" + encoded + "&max_results=" + arxivCount; // 构造 arXiv API 路径
                log.info("arXiv request path: {}", path); // 记录请求路径
                String arxivBody = arxivClient.get()
                        .uri(path)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block(Duration.ofSeconds(40)); // 阻塞等待 arXiv 响应
                log.info("arXiv response length: {}", arxivBody == null ? 0 : arxivBody.length()); // 记录响应长度
                log.info("\n\n\narXiv response content: {}\n\n\n", arxivBody); // 记录完整响应内容（调试用）
                if (arxivBody != null) {
                    int _len = arxivBody.length(); // 响应长度
                    int _show = Math.min(400, _len); // 要显示的片段长度（最多 400）
                    String _snippet = arxivBody.substring(0, _show).replaceAll("\\s+", " "); // 清理空白并截取
                    log.info("arXiv response snippet (first {} chars): {}", _show, _snippet); // 打印响应片段
                }

                if (arxivBody == null) {
                    log.warn("arXiv returned empty body for search {}", ARXIV_SEARCH_QUERY); // 空响应警告
                } else {
                    // 用正则尝试抽取每个 <entry> 元素（简易解析，适用于多数情况）
                    Pattern entryPattern = Pattern.compile("<entry>(.*?)</entry>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE); // 匹配 entry
                    Matcher entryMatcher = entryPattern.matcher(arxivBody); // 创建匹配器
                    while (entryMatcher.find()) { // 遍历每个 entry
                        String entry = entryMatcher.group(1); // 取得 entry 内容
                        String id = null; // 存放 <id>
                        String title = "untitled"; // 存放 <title>
                        Matcher mId = Pattern.compile("<id>(.*?)</id>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(entry); // 匹配 id
                        if (mId.find()) {
                            id = mId.group(1).trim(); // 提取 id
                        }
                        Matcher mTitle = Pattern.compile("<title>(.*?)</title>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(entry); // 匹配 title
                        if (mTitle.find()) {
                            title = mTitle.group(1).trim().replaceAll("\\s+", " "); // 提取并压缩空白
                        }
                        String arxId = null; // arXiv id
                        if (id != null && id.contains("/abs/")) {
                            arxId = id.substring(id.lastIndexOf('/') + 1); // 从 abs URL 提取 id
                        }
                        String pdfUrl = null; // PDF 链接变量
                        if (arxId != null) {
                            pdfUrl = lookupArxivPdfUrl(arxId); // 先用 API 查找带 title 的 link
                            if (pdfUrl == null) {
                                pdfUrl = "https://arxiv.org/pdf/" + arxId + ".pdf"; // 作为回退，构造 pdf URL
                            }
                        }

                        String finalFileId = (arxId != null) ? arxId : ("arx-" + System.currentTimeMillis()); // 回退 fileId

                        if (pdfUrl != null) { // 如果有 PDF 链接，尝试上传
                            try {
                                log.info("Found arXiv PDF for {} -> {}", arxId, pdfUrl); // 记录发现的 PDF
                                String filename = sanitizeFilename(title == null || title.isEmpty() ? finalFileId : title); // 生成文件名
                                String uploadResp = fileServiceClient.uploadFromUrl("datasync", pdfUrl, filename); // 上传并获取响应
                                if (uploadResp != null) {
                                    try {
                                        JsonNode uploadRoot = objectMapper.readTree(uploadResp); // 解析上传响应
                                        JsonNode data = uploadRoot.path("data"); // 读取 data
                                        String uploadedFileId = data.path("fileId").asText(null); // 读取 fileId
                                        if (uploadedFileId != null && !uploadedFileId.isEmpty()) {
                                            finalFileId = uploadedFileId; // 使用上传后返回的 fileId
                                            log.info("Uploaded PDF for arXiv {} to file-service, fileId={}", arxId, finalFileId); // 打印成功信息
                                        } else {
                                            log.warn("Upload response did not contain fileId for arXiv {}: {}", arxId, uploadResp); // 警告：响应缺少 fileId
                                        }
                                    } catch (Exception pe) {
                                        log.warn("Failed to parse upload response for arXiv {}: {}", arxId, pe.getMessage()); // 上传响应解析异常
                                    }
                                } else {
                                    log.warn("File upload returned null for arXiv {}", arxId); // 上传返回 null
                                }
                            } catch (Exception de) {
                                log.warn("Failed to download or upload PDF for arXiv {}: {}", arxId, de.getMessage()); // 下载或上传异常
                            }
                        } else {
                            log.info("No PDF URL found for arXiv {}. Using fallback fileId {}", arxId, finalFileId); // 未找到 PDF，使用回退 ID
                        }

                        Map<String, Object> achPayload = new HashMap<>(); // 构造上报负载
                        achPayload.put("userId", "datasync"); // userId
                        achPayload.put("title", title); // title
                        achPayload.put("fileId", finalFileId); // fileId

                        String jsonPayload = objectMapper.writeValueAsString(achPayload); // 序列化为 JSON
                        log.info("[TEST MODE] Skipping achievement service call for arXiv {} -> payload={}", arxId, jsonPayload); // 测试模式下跳过上报
                    }
                }
            } catch (Exception ae) {
                log.warn("arXiv fetch/upload phase failed: {}", ae.getMessage()); // arXiv 阶段整体异常捕获并警告
            }

            log.info("pullFromPublicDb finished (demo run)"); // 整个 demo 流程完成
        } catch (Exception e) {
            // 捕获并记录异常，包含堆栈信息
            log.error("pullFromPublicDb failed: {}", e.getMessage(), e); // 记录异常
        }
    }

    /**
     * Find a work on OpenAlex by arXiv id and upload its PDF via file-service
     * (streamed). Returns the file-service response body (JSON) or null on
     * failure.
     */
    public String uploadWorkFromOpenAlexByArxiv(String arxivId) { // 根据 arXiv id 在 OpenAlex 查找并上传
        try {
            String body = webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/works")
                    .queryParam("filter", "ids.arxiv:" + arxivId)
                    .queryParam("per-page", "1")
                    .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(10)); // 阻塞等待 OpenAlex 响应

            if (body == null) {
                log.warn("OpenAlex returned empty body for arXiv {}", arxivId); // 空响应
                return null; // 无结果
            }

            JsonNode root = objectMapper.readTree(body); // 解析 JSON
            JsonNode results = root.path("results"); // 获取 results
            if (!results.isArray() || results.size() == 0) {
                log.warn("No OpenAlex work found for arXiv {}", arxivId); // 未找到对应 work
                return null; // 返回 null
            }

            JsonNode work = results.get(0); // 取第一个匹配的 work
            String title = work.path("title").asText("untitled"); // 取标题
            String pdfUrl = extractPdfUrl(work); // 提取 pdf URL
            if (pdfUrl == null) {
                log.warn("No PDF URL found for arXiv {}", arxivId); // 没有 PDF
                return null; // 返回 null
            }

            String filename = sanitizeFilename(title == null || title.isEmpty() ? arxivId : title); // 生成文件名
            log.info("Uploading from OpenAlex arXiv {} -> url={} filename={}", arxivId, pdfUrl, filename); // 记录上传行为
            return fileServiceClient.uploadFromUrl("datasync", pdfUrl, filename); // 上传并返回响应

        } catch (Exception e) {
            log.error("uploadWorkFromOpenAlexByArxiv failed for {}: {}", arxivId, e.getMessage(), e); // 异常记录
            return null; // 返回 null
        }
    }

    /**
     * Deterministic helper for tests: given an arXiv id, try to resolve a PDF
     * URL (via arXiv API lookup) and upload it to file-service. Returns the raw
     * file-service response body (JSON) or null on failure.
     */
    public String uploadFromArxivById(String arxivId) { // 给定 arXiv id，尝试查找 PDF 并上传（测试用）
        if (arxivId == null || arxivId.isEmpty()) {
            return null; // 参数检查
        }
        try {
            String pdfUrl = lookupArxivPdfUrl(arxivId); // 通过 arXiv API 查找 PDF 链接
            if (pdfUrl == null) {
                pdfUrl = arxivPdfFromId(arxivId); // 回退到构造的 pdf URL
            }
            if (pdfUrl == null) {
                return null; // 无法获取 PDF
            }

            String filename = sanitizeFilename(arxivId + ".pdf"); // 文件名
            log.info("Uploading arXiv {} -> {}", arxivId, pdfUrl); // 记录上传动作
            return fileServiceClient.uploadFromUrl("datasync", pdfUrl, filename); // 上传并返回响应
        } catch (Exception e) {
            log.error("uploadFromArxivById failed for {}: {}", arxivId, e.getMessage(), e); // 异常记录
            return null; // 返回 null
        }
    }

    /**
     * Try multiple strategies to extract a PDF URL from an OpenAlex work JSON
     * node. Order of attempts: 1) best_oa_location.url_for_pdf /
     * best_oa_location.url 2) primary_location.url_for_pdf /
     * primary_location.url 3) oa_url field 4) ids.arxiv or any arXiv identifier
     * -> construct https://arxiv.org/pdf/{id}.pdf 5) look for any url-like
     * fields on the work
     */
    private String extractPdfUrl(JsonNode work) { // 从 OpenAlex 的 work 节点中尝试提取 PDF 链接
        if (work == null || work.isMissingNode()) {
            return null; // 空节点
        }

        // 1) 尝试 best_oa_location 下的 url_for_pdf 或 url
        JsonNode best = work.path("best_oa_location");
        if (!best.isMissingNode()) {
            String u = textOrNull(best, "url_for_pdf"); // 取 url_for_pdf
            if (isPdfUrl(u)) {
                return u; // 是 PDF 链接则返回
            }
            u = textOrNull(best, "url"); // 否则尝试 url
            if (isPdfUrl(u)) {
                return u; // 返回
            }
        }

        // 2) 尝试 primary_location
        JsonNode primary = work.path("primary_location");
        if (!primary.isMissingNode()) {
            String u = textOrNull(primary, "url_for_pdf"); // 取 url_for_pdf
            if (isPdfUrl(u)) {
                return u;
            }
            u = textOrNull(primary, "url"); // 取 url
            if (isPdfUrl(u)) {
                return u;
            }
        }

        // 2b) 尝试 oa_locations 数组中所有位置
        JsonNode oaLocations = work.path("oa_locations");
        if (oaLocations != null && oaLocations.isArray()) {
            for (JsonNode loc : oaLocations) {
                String u = textOrNull(loc, "url_for_pdf"); // 取 url_for_pdf
                if (isPdfUrl(u)) {
                    return u; // 返回
                }
                u = textOrNull(loc, "url"); // 取 url
                if (isPdfUrl(u)) {
                    return u; // 返回
                }
            }
        }

        // 3) 尝试 oa_url 字段
        String oa = textOrNull(work, "oa_url");
        if (isPdfUrl(oa)) {
            return oa; // 返回 oa_url
        }

        // 4) 尝试 ids 字段中是否含有 arXiv id
        JsonNode ids = work.path("ids");
        if (!ids.isMissingNode()) {
            // 如果存在专门的 arxiv 字段，直接取出
            if (ids.has("arxiv")) {
                String arx = ids.path("arxiv").asText(null); // 取 arxiv 值
                if (arx != null && !arx.isEmpty()) {
                    return arxivPdfFromId(arx); // 构造 pdf URL 并返回
                }
            }
            // 否则枚举 ids 下的所有字段，寻找看起来像 arXiv id 的值
            Iterator<String> idFields = ids.fieldNames();
            while (idFields.hasNext()) {
                String key = idFields.next(); // 字段名
                String v = ids.path(key).asText(null); // 字段值
                if (v != null && looksLikeArXivId(v)) {
                    return arxivPdfFromId(v); // 如果像 arXiv id，则构造并返回 pdf URL
                }
            }
        }

        // 5) 在常见的 top-level 字段中查找 URL 或 arXiv abs 链接
        String[] candidates = {textOrNull(work, "url"), textOrNull(work, "id"), textOrNull(work, "uri")}; // 候选字段
        for (String c : candidates) {
            if (isPdfUrl(c)) {
                return c; // 直接返回 PDF URL
            }
            // 如果是 arXiv 的 abs URL，则转换为 pdf
            if (c != null && c.contains("arxiv.org/abs/")) {
                String arx = c.substring(c.lastIndexOf('/') + 1); // 提取 id
                return arxivPdfFromId(arx); // 返回构造的 pdf URL
            }
        }

        return null; // 未找到任何合适的 PDF 链接
    }

    private String textOrNull(JsonNode node, String field) { // 安全读取字段值，若为空则返回 null
        if (node == null || node.isMissingNode()) {
            return null; // 空节点返回 null
        }
        String v = node.path(field).asText(null); // 直接尝试读取字段
        return (v == null || v.isEmpty()) ? null : v; // 空串返回 null
    }

    private boolean isPdfUrl(String u) { // 简单判断字符串是否看起来像 PDF 链接
        if (u == null) {
            return false; // null 不是 PDF
        }
        String lower = u.toLowerCase(); // 小写比较
        return lower.endsWith(".pdf") || lower.contains("/pdf") || lower.contains("arxiv.org/pdf") || (lower.contains("drive.google.com") && lower.contains("export=download")); // 包含常见 PDF 标记
    }

    private boolean looksLikeArXivId(String s) { // 判断字符串是否像 arXiv id
        if (s == null) {
            return false; // null 不是 arXiv id
        }
        // 基本模式匹配：形如 1706.03762 或 arXiv:1706.03762，或包含 arxiv 字样
        return s.matches("(?i)(arxiv:)?\\d{4}\\.\\d{4,5}(|v\\d+)") || s.toLowerCase().contains("arxiv");
    }

    private String arxivPdfFromId(String arx) { // 根据 arXiv id 构造 pdf 下载链接
        if (arx == null) {
            return null; // null 检查
        }
        // 规范化：去掉前缀 arXiv:
        arx = arx.replaceAll("(?i)arxiv:", "").trim(); // 去掉 arxiv: 前缀并修剪
        // 如果包含路径如 abs/xxx，则取最后一段
        if (arx.contains("/")) {
            arx = arx.substring(arx.lastIndexOf('/') + 1); // 取最后一段
        }
        return "https://arxiv.org/pdf/" + arx + ".pdf"; // 返回构造的 PDF URL
    }

    /**
     * Query arXiv API (Atom) for the given arXiv id and try to extract a PDF
     * URL. Returns PDF href if found, otherwise null.
     */
    private String lookupArxivPdfUrl(String arx) { // 通过 arXiv 的 id_list API 查询条目并从 link 中提取 pdf href
        if (arx == null || arx.isEmpty()) {
            return null; // 参数检查
        }
        try {
            String body = arxivClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/query").queryParam("id_list", arx).build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(10)); // 阻塞等待响应

            if (body == null) {
                return null; // 空响应
            }

            Document doc = parseXml(body); // 解析为 DOM
            if (doc == null) {
                return null; // 解析失败
            }
            NodeList entries = doc.getElementsByTagName("entry"); // 获取 entry 节点集合
            for (int i = 0; i < entries.getLength(); i++) { // 遍历每个 entry
                Element entry = (Element) entries.item(i); // 当前 entry
                NodeList links = entry.getElementsByTagName("link"); // 取 link 节点
                for (int j = 0; j < links.getLength(); j++) { // 遍历 link
                    Element link = (Element) links.item(j);
                    String href = link.getAttribute("href"); // href 属性
                    String title = link.getAttribute("title"); // title 属性
                    String type = link.getAttribute("type"); // type 属性
                    if ((title != null && title.equalsIgnoreCase("pdf")) || (type != null && type.toLowerCase().contains("pdf")) || (href != null && href.contains("/pdf/"))) {
                        return href; // 如果 link 标记为 pdf 或 href 中包含 /pdf/ 则返回 href
                    }
                }
            }

        } catch (Exception e) {
            log.warn("lookupArxivPdfUrl error for {}: {}", arx, e.getMessage()); // 异常时记录警告
        }
        return null; // 未找到 PDF
    }

    private Document parseXml(String xml) { // 将 XML 字符串解析为 DOM Document，并禁止外部实体以提高安全性
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance(); // 创建工厂
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); // 禁用 DOCTYPE
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false); // 禁用外部通用实体
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false); // 禁用外部参数实体
            DocumentBuilder db = dbf.newDocumentBuilder(); // 创建解析器
            InputSource is = new InputSource(new StringReader(xml)); // 用字符串创建输入源
            return db.parse(is); // 解析并返回 Document
        } catch (Exception e) {
            log.warn("parseXml failed: {}", e.getMessage()); // 解析失败记录警告
            return null; // 返回 null
        }
    }

    private String encodeArxivQuery(String q) { // 对 arXiv 查询字符串进行 URL 编码
        if (q == null) {
            return ""; // 空查询返回空串
        }
        return URLEncoder.encode(q, StandardCharsets.UTF_8); // 使用 UTF-8 编码
    }

    /**
     * Sanitize a title to a safe filename. Replaces invalid characters with
     * underscores and ensures a .pdf extension.
     */
    private String sanitizeFilename(String title) { // 将标题转换为安全的文件名
        if (title == null) {
            return "untitled.pdf"; // 空标题回退
        }
        String s = title.trim(); // 去除首尾空白
        // 替换路径分隔符和控制字符为下划线
        s = s.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]+", "_");
        // 折叠多个空白为单个下划线
        s = s.replaceAll("\\s+", "_");
        if (!s.toLowerCase().endsWith(".pdf")) {
            s = s + ".pdf"; // 确保以 .pdf 结尾
        }
        // 限制文件名长度以避免过长
        if (s.length() > 200) {
            s = s.substring(0, 200); // 截断到 200 字符
        }
        return s; // 返回清理后的文件名
    }
}
