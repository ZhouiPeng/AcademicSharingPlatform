// 包声明：定义当前类所在的包，表示这个实现属于 data-sync-service 的服务实现包
package com.academic.datasync.service.impl; // 定义包名

// 引入需要的类与接口，下面每行都附带中文注释以说明用途
import java.io.StringReader; // 用于把字符串包装为 Reader 以便 XML 解析
import java.net.URLEncoder; // 用于对 arXiv 查询字符串进行 URL 编码
import java.nio.charset.StandardCharsets; // 提供标准字符集常量（UTF-8）
import java.time.Duration; // 表示时间段，用于 WebClient 的超时
import java.util.HashMap; // 提供 HashMap 实现
import java.util.Iterator; // 用于遍历集合的迭代器
import java.util.List; // Map 接口，用于构建上报负载
import java.util.Map; // 正则匹配器
import java.util.regex.Matcher; // 正则模式
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory; // DOM 解析器的构建器

import org.slf4j.Logger; // DOM DocumentBuilder 的工厂
import org.slf4j.LoggerFactory; // 日志接口
import org.springframework.beans.factory.annotation.Value; // 用于注入配置开关与列表
import org.springframework.scheduling.annotation.Scheduled; // 用于计划任务注解
import org.springframework.stereotype.Service; // 日志工厂，用于创建 Logger
import org.springframework.web.reactive.function.client.WebClient; // 标注当前类为 Spring 的 Service 组件
import org.w3c.dom.Document; // 非阻塞的 HTTP 客户端
import org.w3c.dom.Element; // DOM Document 表示解析后的 XML 文档
import org.w3c.dom.NodeList; // DOM Element 表示 XML 元素
import org.xml.sax.InputSource; // DOM NodeList 表示节点列表

import com.academic.datasync.client.AchievementServiceClient; // 将字符串包装为 InputSource 供解析器使用
import com.academic.datasync.client.FileServiceClient; // 成就服务客户端接口（注入）
import com.academic.datasync.service.DataSyncService; // 文件服务客户端接口（注入）
import com.fasterxml.jackson.databind.JsonNode; // DataSync 服务接口
import com.fasterxml.jackson.databind.ObjectMapper; // Jackson 的 JsonNode，用于 JSON 树解析

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

    // 自动爬取相关配置（从 application.yml 注入）
    @Value("${datasync.auto-enabled:false}")
    private boolean autoCrawlEnabled; // 开关：为 true 时启用定时爬取

    @Value("${datasync.auto-categories:cs.AI,cs.CL,cs.LG}")
    private String autoCategories; // 逗号分隔的领域列表

    @Value("${datasync.per-category-count:10}")
    private int perCategoryCount; // 每个领域拉取数量，默认 10

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

    /**
     * Scheduled trigger: every day at 02:00 (server local time). Will run only
     * when `datasync.auto-enabled` is true. Method delegates to
     * `pullFromPublicDb()` which performs the multi-category crawl.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void scheduledAutoCrawl() {
        if (!autoCrawlEnabled) {
            log.info("Auto-crawl disabled; skipping scheduled run");
            return;
        }
        log.info("Scheduled auto-crawl triggered at 02:00");
        try {
            pullFromPublicDb();
        } catch (Exception e) {
            log.error("Scheduled auto-crawl failed: {}", e.getMessage(), e);
        }
    }

    @Override
    public void pullFromPublicDb() { // 从公共数据库（OpenAlex 和 arXiv）拉取并处理示例流程
        log.info("Starting pullFromPublicDb: fetching works from OpenAlex (demo limited)"); // 记录开始
        try {
            // 对每个配置的领域执行 OpenAlex 拉取，每个领域拉取 `perCategoryCount` 条记录
            String[] categories = autoCategories.split("\\s*,\\s*");
            for (String cat : categories) {
                try {
                    log.info("Fetching OpenAlex works for category {} (limit={})", cat, perCategoryCount);
                    String body = webClient.get()
                            .uri(uriBuilder -> uriBuilder.path("/works")
                            .queryParam("filter", "is_oa:true,concepts.display_name:" + cat)
                            .queryParam("per-page", String.valueOf(perCategoryCount))
                            .build())
                            .retrieve()
                            .bodyToMono(String.class)
                            .block(Duration.ofSeconds(10)); // 阻塞等待响应

                    if (body == null) {
                        log.warn("OpenAlex returned empty body for category {}", cat);
                        continue;
                    }

                    JsonNode root = objectMapper.readTree(body);
                    JsonNode results = root.get("results");
                    if (results == null || !results.isArray()) {
                        log.warn("Unexpected OpenAlex response structure for category {}", cat);
                        continue;
                    }

                    Iterator<JsonNode> it = results.elements();
                    while (it.hasNext()) {
                        JsonNode work = it.next();
                        String title = work.path("title").asText("untitled");
                        String openalexId = work.path("id").asText();
                        String doi = work.path("doi").asText(null);

                        String pdfUrl = extractPdfUrl(work);
                        String fallbackFileId = openalexId == null || openalexId.isEmpty() ? (doi != null ? doi : "oa-" + System.currentTimeMillis()) : openalexId;
                        String finalFileId = fallbackFileId;

                        if (pdfUrl != null && !pdfUrl.isEmpty()) {
                            try {
                                log.info("Found PDF URL for work {}: {}", openalexId, pdfUrl);
                                String rawTitle = title == null || title.isEmpty() ? finalFileId : title;
                                String filename = sanitizeFilename(rawTitle);
                                String uploadResp = fileServiceClient.uploadFromUrl("datasync", pdfUrl, filename);
                                if (uploadResp != null) {
                                    try {
                                        JsonNode uploadRoot = objectMapper.readTree(uploadResp);
                                        JsonNode data = uploadRoot.path("data");
                                        String uploadedFileId = data.path("fileId").asText(null);
                                        if (uploadedFileId != null && !uploadedFileId.isEmpty()) {
                                            finalFileId = uploadedFileId;
                                            log.info("Uploaded PDF for work {} to file-service, fileId={}", openalexId, finalFileId);

                                            List<String> authors = extractAuthorsFromWork(work);
                                            String abstractText = textOrNull(work, "abstract");
                                            String achJson = buildAchievementJson(title, authors, abstractText, finalFileId, null);
                                            String achId = null;
                                            try {
                                                achId = achievementClient.createAchievement(achJson);
                                            } catch (Exception ex) {
                                                log.warn("createAchievement threw for work {}: {}", openalexId, ex.getMessage());
                                            }
                                            if (achId == null || achId.isEmpty()) {
                                                log.warn("Achievement creation failed for work {}. Deleting uploaded file {}", openalexId, finalFileId);
                                                try {
                                                    fileServiceClient.deleteFile(finalFileId);
                                                } catch (Exception ex) {
                                                    log.error("Failed to delete file {} after achievement failure: {}", finalFileId, ex.getMessage());
                                                }
                                            } else {
                                                log.info("Created achievement {} for work {}", achId, openalexId);
                                            }
                                        } else {
                                            log.warn("Upload response did not contain fileId for work {}: {}", openalexId, uploadResp);
                                        }
                                    } catch (Exception pe) {
                                        log.warn("Failed to parse upload response for work {}: {}", openalexId, pe.getMessage());
                                    }
                                } else {
                                    log.warn("File upload returned null for work {}", openalexId);
                                }
                            } catch (Exception de) {
                                log.warn("Failed to download or upload PDF for work {}: {}", openalexId, de.getMessage());
                            }
                        } else {
                            log.info("No PDF URL found for work {}. Using fallback fileId {}", openalexId, finalFileId);
                        }

                        Map<String, Object> achPayload = new HashMap<>();
                        achPayload.put("userId", "datasync");
                        achPayload.put("title", title);
                        achPayload.put("fileId", finalFileId);
                        String jsonPayload = objectMapper.writeValueAsString(achPayload);
                        log.info("[TEST MODE] Skipping achievement service call for work {} -> payload={}", openalexId, jsonPayload);
                    }
                } catch (Exception e) {
                    log.warn("OpenAlex fetch/upload phase failed for category {}: {}", cat, e.getMessage());
                }
            }

            // OpenAlex 部分处理完成，接着尝试从 arXiv 拉取记录（按每个领域拉取 `perCategoryCount` 条）
            int arxivCount = perCategoryCount > 0 ? perCategoryCount : 10;
            String[] cats = autoCategories.split("\\s*,\\s*");
            try {
                for (String cat : cats) {
                    final String ARXIV_SEARCH_QUERY = "cat:" + cat;
                    log.info("arXiv query for category {} (max_results={})", cat, arxivCount);
                    String arxivBody = arxivClient.get()
                            .uri(uriBuilder -> uriBuilder.path("/api/query")
                            .queryParam("search_query", ARXIV_SEARCH_QUERY)
                            .queryParam("max_results", arxivCount)
                            .build())
                            .retrieve()
                            .bodyToMono(String.class)
                            .block(Duration.ofSeconds(40));

                    log.info("arXiv response length for {}: {}", cat, arxivBody == null ? 0 : arxivBody.length());
                    if (arxivBody != null) {
                        int _len = arxivBody.length();
                        int _show = Math.min(400, _len);
                        String _snippet = arxivBody.substring(0, _show).replaceAll("\\s+", " ");
                        log.info("arXiv response snippet for {} (first {} chars): {}", cat, _show, _snippet);
                    }

                    if (arxivBody == null) {
                        log.warn("arXiv returned empty body for search {}", ARXIV_SEARCH_QUERY);
                        continue;
                    }

                    Pattern entryPattern = Pattern.compile("<entry>(.*?)</entry>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
                    Matcher entryMatcher = entryPattern.matcher(arxivBody);
                    while (entryMatcher.find()) {
                        String entry = entryMatcher.group(1);
                        String id = null;
                        String title = "untitled";
                        Matcher mId = Pattern.compile("<id>(.*?)</id>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(entry);
                        if (mId.find()) {
                            id = mId.group(1).trim();
                        }
                        Matcher mTitle = Pattern.compile("<title>(.*?)</title>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(entry);
                        if (mTitle.find()) {
                            title = mTitle.group(1).trim().replaceAll("\\s+", " ");
                        }
                        String arxId = null;
                        if (id != null && id.contains("/abs/")) {
                            arxId = id.substring(id.lastIndexOf('/') + 1);
                        }
                        String pdfUrl = null;
                        if (arxId != null) {
                            pdfUrl = lookupArxivPdfUrl(arxId);
                            if (pdfUrl == null) {
                                pdfUrl = "https://arxiv.org/pdf/" + arxId + ".pdf";
                            }
                        }

                        String finalFileId = (arxId != null) ? arxId : ("arx-" + System.currentTimeMillis());

                        if (pdfUrl != null) {
                            try {
                                log.info("Found arXiv PDF for {} -> {}", arxId, pdfUrl);
                                String filename = sanitizeFilename(title == null || title.isEmpty() ? finalFileId : title);
                                String uploadResp = fileServiceClient.uploadFromUrl("datasync", pdfUrl, filename);
                                if (uploadResp != null) {
                                    try {
                                        JsonNode uploadRoot = objectMapper.readTree(uploadResp);
                                        JsonNode data = uploadRoot.path("data");
                                        String uploadedFileId = data.path("fileId").asText(null);
                                        if (uploadedFileId != null && !uploadedFileId.isEmpty()) {
                                            finalFileId = uploadedFileId;
                                            log.info("Uploaded PDF for arXiv {} to file-service, fileId={}", arxId, finalFileId);
                                            java.util.List<String> authors = extractAuthorsFromArxivEntry(entry);
                                            String abstractText = extractSummaryFromArxivEntry(entry);
                                            String achJson = buildAchievementJson(title, authors, abstractText, finalFileId, null);
                                            String achId = null;
                                            try {
                                                achId = achievementClient.createAchievement(achJson);
                                            } catch (Exception ex) {
                                                log.warn("createAchievement threw for arXiv {}: {}", arxId, ex.getMessage());
                                            }
                                            if (achId == null || achId.isEmpty()) {
                                                log.warn("Achievement creation failed for arXiv {}. Deleting uploaded file {}", arxId, finalFileId);
                                                try {
                                                    fileServiceClient.deleteFile(finalFileId);
                                                } catch (Exception ex) {
                                                    log.error("Failed to delete file {} after achievement failure: {}", finalFileId, ex.getMessage());
                                                }
                                            } else {
                                                log.info("Created achievement {} for arXiv {}", achId, arxId);
                                            }
                                        } else {
                                            log.warn("Upload response did not contain fileId for arXiv {}: {}", arxId, uploadResp);
                                        }
                                    } catch (Exception pe) {
                                        log.warn("Failed to parse upload response for arXiv {}: {}", arxId, pe.getMessage());
                                    }
                                } else {
                                    log.warn("File upload returned null for arXiv {}", arxId);
                                }
                            } catch (Exception de) {
                                log.warn("Failed to download or upload PDF for arXiv {}: {}", arxId, de.getMessage());
                            }
                        } else {
                            log.info("No PDF URL found for arXiv {}. Using fallback fileId {}", arxId, finalFileId);
                        }

                        Map<String, Object> achPayload = new HashMap<>();
                        achPayload.put("userId", "datasync");
                        achPayload.put("title", title);
                        achPayload.put("fileId", finalFileId);

                        String jsonPayload = objectMapper.writeValueAsString(achPayload);
                        log.info("[TEST MODE] Skipping achievement service call for arXiv {} -> payload={}", arxId, jsonPayload);
                    }
                }
            } catch (Exception ae) {
                log.warn("arXiv fetch/upload phase failed: {}", ae.getMessage());
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
            String uploadResp = fileServiceClient.uploadFromUrl("datasync", pdfUrl, filename);
            if (uploadResp == null) {
                return null;
            }
            try {
                JsonNode uploadRoot = objectMapper.readTree(uploadResp);
                JsonNode data = uploadRoot.path("data");
                String uploadedFileId = data.path("fileId").asText(null);
                if (uploadedFileId != null && !uploadedFileId.isEmpty()) {
                    List<String> authors = extractAuthorsFromWork(work);
                    String abstractText = textOrNull(work, "abstract");
                    String achJson = buildAchievementJson(title, authors, abstractText, uploadedFileId, null);
                    String achId = null;
                    try {
                        achId = achievementClient.createAchievement(achJson);
                    } catch (Exception ex) {
                        log.warn("createAchievement threw for arXiv {}: {}", arxivId, ex.getMessage());
                    }
                    if (achId == null || achId.isEmpty()) {
                        log.warn("Achievement creation failed for {}. Deleting uploaded file {}", arxivId, uploadedFileId);
                        try {
                            fileServiceClient.deleteFile(uploadedFileId);
                        } catch (Exception ex) {
                            log.error("Failed to delete file {}: {}", uploadedFileId, ex.getMessage());
                        }
                        return null;
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse upload response for arXiv {}: {}", arxivId, e.getMessage());
            }
            return uploadResp;

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
            String uploadResp = fileServiceClient.uploadFromUrl("datasync", pdfUrl, filename); // 上传并返回响应
            if (uploadResp == null) {
                return null;
            }
            try {
                JsonNode uploadRoot = objectMapper.readTree(uploadResp);
                JsonNode data = uploadRoot.path("data");
                String uploadedFileId = data.path("fileId").asText(null);
                if (uploadedFileId != null && !uploadedFileId.isEmpty()) {
                    java.util.List<String> authors = new java.util.ArrayList<>();
                    String abstractText = null;
                    String achJson = buildAchievementJson(arxivId, authors, abstractText, uploadedFileId, null);
                    String achId = null;
                    try {
                        achId = achievementClient.createAchievement(achJson);
                    } catch (Exception ex) {
                        log.warn("createAchievement threw for arXiv {}: {}", arxivId, ex.getMessage());
                    }
                    if (achId == null || achId.isEmpty()) {
                        log.warn("Achievement creation failed for {}. Deleting uploaded file {}", arxivId, uploadedFileId);
                        try {
                            fileServiceClient.deleteFile(uploadedFileId);
                        } catch (Exception ex) {
                            log.error("Failed to delete file {}: {}", uploadedFileId, ex.getMessage());
                        }
                        return null;
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse upload response for arXiv {}: {}", arxivId, e.getMessage());
            }
            return uploadResp;
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

    // 从 OpenAlex work 节点尽量提取作者列表
    private java.util.List<String> extractAuthorsFromWork(JsonNode work) {
        java.util.List<String> authors = new java.util.ArrayList<>();
        if (work == null || work.isMissingNode()) {
            return authors;
        }
        JsonNode auths = work.path("authorships");
        if (auths != null && auths.isArray()) {
            for (JsonNode a : auths) {
                String name = null;
                if (a.has("author")) {
                    JsonNode au = a.path("author");
                    name = au.path("display_name").asText(null);
                    if (name == null) {
                        name = au.path("author_display_name").asText(null);
                    }
                }
                if (name == null) {
                    name = a.path("raw_affiliation_string").asText(null);
                }
                if (name == null) {
                    name = a.path("display_name").asText(null);
                }
                if (name != null && !name.isEmpty() && !authors.contains(name)) {
                    authors.add(name);
                }
            }
        }
        return authors;
    }

    // 从 arXiv entry 文本中尝试提取作者列表（<author><name>）
    private java.util.List<String> extractAuthorsFromArxivEntry(String entry) {
        java.util.List<String> authors = new java.util.ArrayList<>();
        if (entry == null) {
            return authors;
        }
        Pattern p = Pattern.compile("<author>.*?<name>(.*?)</name>.*?</author>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(entry);
        while (m.find()) {
            String n = m.group(1).trim().replaceAll("\\s+", " ");
            if (!n.isEmpty() && !authors.contains(n)) {
                authors.add(n);
            }
        }
        return authors;
    }

    // 提取 arXiv entry 的 <summary>
    private String extractSummaryFromArxivEntry(String entry) {
        if (entry == null) {
            return null;
        }
        Matcher m = Pattern.compile("<summary>(.*?)</summary>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(entry);
        if (m.find()) {
            return m.group(1).trim().replaceAll("\\s+", " ");
        }
        return null;
    }

    // 构造符合 AchievementDto 的 JSON 字符串
    private String buildAchievementJson(String title, java.util.List<String> authors, String abstractText, String fileId, Integer type) {
        try {
            com.fasterxml.jackson.databind.node.ObjectNode node = objectMapper.createObjectNode();
            node.putNull("achievementId");
            node.put("userId", "datasync");
            node.put("title", title == null ? "" : title);
            if (type == null) {
                node.putNull("type");
            } else {
                node.put("type", type);
            }
            if (authors != null) {
                com.fasterxml.jackson.databind.node.ArrayNode arr = node.putArray("authors");
                for (String a : authors) {
                    arr.add(a);
                }
            } else {
                node.putArray("authors");
            }
            node.put("abstract", abstractText == null ? "" : abstractText);
            node.put("fileId", fileId == null ? "" : fileId);
            node.put("createdAt", System.currentTimeMillis());
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            log.warn("buildAchievementJson failed: {}", e.getMessage());
            return null;
        }
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
