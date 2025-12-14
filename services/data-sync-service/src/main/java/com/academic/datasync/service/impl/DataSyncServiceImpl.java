// 包声明：定义当前类所在的包，表示这个实现属于 data-sync-service 的服务实现包
package com.academic.datasync.service.impl; // 定义包名

// 引入需要的类与接口，下面每行都附带中文注释以说明用途
import java.io.IOException; // 用于把字符串包装为 Reader 以便 XML 解析
import java.io.StringReader; // 用于对 arXiv 查询字符串进行 URL 编码
import java.net.URLEncoder; // 提供标准字符集常量（UTF-8）
import java.nio.charset.StandardCharsets; // 表示时间段，用于 WebClient 的超时
import java.nio.file.Files; // 提供 HashMap 实现
import java.nio.file.Path; // 用于遍历集合的迭代器
import java.nio.file.Paths; // Map 接口，用于构建上报负载
import java.time.Duration; // 正则匹配器
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map; // 正则模式
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger; // DOM 解析器的构建器
import org.slf4j.LoggerFactory; // DOM DocumentBuilder 的工厂
import org.springframework.beans.factory.annotation.Value; // 日志接口
import org.springframework.scheduling.annotation.Scheduled; // 用于注入配置开关与列表
import org.springframework.stereotype.Service; // 用于计划任务注解
import org.springframework.web.reactive.function.client.WebClient; // 日志工厂，用于创建 Logger
import org.w3c.dom.Document; // 标注当前类为 Spring 的 Service 组件
import org.w3c.dom.Element; // 非阻塞的 HTTP 客户端
import org.w3c.dom.NodeList; // DOM Document 表示解析后的 XML 文档
import org.xml.sax.InputSource; // DOM Element 表示 XML 元素

import com.academic.datasync.client.AchievementServiceClient; // DOM NodeList 表示节点列表
import com.academic.datasync.client.FileServiceClient; // 将字符串包装为 InputSource 供解析器使用
import com.academic.datasync.service.DataSyncService; // 成就服务客户端接口（注入）
import com.fasterxml.jackson.databind.JsonNode; // 文件服务客户端接口（注入）
import com.fasterxml.jackson.databind.ObjectMapper; // DataSync 服务接口

@Service // 声明这是一个 Spring 管理的服务组件
public class DataSyncServiceImpl implements DataSyncService { // 实现 DataSyncService 接口

    // 日志记录器，用于输出运行时信息
    private static final Logger log = LoggerFactory.getLogger(DataSyncServiceImpl.class); // 获取当前类的 Logger

    // WebClient 实例用于访问 OpenAlex API（元数据）
    private final WebClient openAlexClient; // 用于调用 OpenAlex 的客户端
    // arXiv 的 WebClient 用于获取 Atom feed
    private final WebClient arxivClient; // 用于调用 arXiv API 的客户端
    // JSON 解析器实例
    private final ObjectMapper objectMapper = new ObjectMapper(); // Jackson 的 ObjectMapper
    // 成就服务客户端（注入）
    private final AchievementServiceClient achievementClient; // 注入用于上报成就的客户端
    // 文件服务客户端（注入）
    private final FileServiceClient fileServiceClient; // 注入用于上传文件的客户端
    private Map<String, String> domainMap = new HashMap<String, String>() {
        {
            // 物理学相关映射（OpenAlex: Physics (C121332964) + arXiv: physics, astro-ph, gr-qc, hep-ex, hep-lat, hep-ph, hep-th, nucl-ex, nucl-th, quant-ph）
            put("C121332964", "物理学");
            put("physics", "物理学");
            put("astro-ph", "物理学");
            put("gr-qc", "物理学");
            put("hep-ex", "物理学");
            put("hep-lat", "物理学");
            put("hep-ph", "物理学");
            put("hep-th", "物理学");
            put("nucl-ex", "物理学");
            put("nucl-th", "物理学");
            put("quant-ph", "物理学");

            // 计算机科学（OpenAlex: Computer Science (C41008148) + arXiv: cs）
            put("C41008148", "计算机科学");
            put("cs", "计算机科学");

            // 数学（OpenAlex: Mathematics (C33923547) + arXiv: math, math-ph, stat）
            put("C33923547", "数学");
            put("math", "数学");
            put("math-ph", "数学");
            put("stat", "数学");

            // 材料科学（OpenAlex: Materials Science (C144133960)）
            put("C144133960", "材料科学");

            // 化学（OpenAlex: Chemistry (C185592680)）
            put("C185592680", "化学");

            // 生物学（OpenAlex: Biology (C86803240) + arXiv: q-bio）
            put("C86803240", "生物学");
            put("q-bio", "生物学");

            // 工程学（OpenAlex: Engineering (C127413603) + arXiv: eess）
            put("C127413603", "工程学");
            put("eess", "工程学");

            // 经济学（OpenAlex: Economics (C16203183) + arXiv: econ, q-fin）
            put("C16203183", "经济学");
            put("econ", "经济学");
            put("q-fin", "经济学");

            // 商业（OpenAlex: Business (C106769008)）
            put("C106769008", "商业");

            // 政治学（OpenAlex: Political Science (C17773945)）
            put("C17773945", "政治学");

            // 社会学（OpenAlex: Sociology (C58743932)）
            put("C58743932", "社会学");

            // 心理学（OpenAlex: Psychology (C95457728)）
            put("C95457728", "心理学");

            // 哲学（OpenAlex: Philosophy (C127961042)）
            put("C127961042", "哲学");

            // 历史学（OpenAlex: History (C112351118)）
            put("C112351118", "历史学");

            // 艺术（OpenAlex: Art (C15744967)）
            put("C15744967", "艺术");

            // 医学（OpenAlex: Medicine (C71924100)）
            put("C71924100", "医学");

            // 环境科学（OpenAlex: Environmental Science (C39432304)）
            put("C39432304", "环境科学");

            // 地理学（OpenAlex: Geography (C162324750)）
            put("C162324750", "地理学");

            // 地质学（OpenAlex: Geology (C138885662)）
            put("C138885662", "地质学");

            // 凝聚态物理（arXiv: cond-mat）
            put("cond-mat", "凝聚态物理");

            // 非线性科学（arXiv: nlin）
            put("nlin", "非线性科学");
        }
    };

    // 自动爬取相关配置（从 application.yml 注入）
    @Value("${datasync.auto-enabled:false}")
    private boolean autoCrawlEnabled; // 开关：为 true 时启用定时爬取

    @Value("${datasync.auto-categories:astro-ph, cond-mat, cs, econ, eess, gr-qc, hep-ex, hep-lat, hep-ph, hep-th, math, math-ph, nlin, nucl-ex, nucl-th, physics, q-bio, q-fin, quant-ph, stat}")
    private String autoCategories; // 逗号分隔的领域列表

    @Value("${datasync.per-category-count:10}")
    private int perCategoryCount; // 每个领域拉取数量，默认 10

    // OpenAlex field IDs (comma-separated). Defaults to Computer Science top-level ID.
    @Value("${datasync.openalex-field-ids:C41008148, C71924100, C86803240, C185592680, C121332964, C39432304, C144133960, C127413603, C33923547, C162324750, C138885662, C95457728, C106769008, C16203183, C17773945, C58743932, C15744967, C112351118, C127961042}")
    private String openAlexFieldIds;

    // persistence for progress and processed ids
    private final Path progressFile = Paths.get("datasync_progress.json");
    private final Path processedFile = Paths.get("datasync_processed.json");
    private final Map<String, Integer> progressMap = new ConcurrentHashMap<>();
    private final Set<String> processedIds = ConcurrentHashMap.newKeySet();

    // 构造函数：通过 Spring 注入 WebClient.Builder 和客户端实现
    public DataSyncServiceImpl(WebClient.Builder builder,
            AchievementServiceClient achievementClient,
            FileServiceClient fileServiceClient) {
        // 为 WebClient 配置 exchange strategies（增加内存缓存上限，避免大响应导致 OOM）
        org.springframework.web.reactive.function.client.ExchangeStrategies strategies
                = org.springframework.web.reactive.function.client.ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(5 * 1024 * 1024))
                        .build(); // 设置最大内存缓冲为 5 MiB
        this.openAlexClient = builder.exchangeStrategies(strategies).baseUrl("https://api.openalex.org").build(); // OpenAlex 基础 URL
        this.arxivClient = org.springframework.web.reactive.function.client.WebClient.builder()
                .exchangeStrategies(strategies)
                .defaultHeader("User-Agent", "AcademicSharingPlatform/0.1 (github:AcademicSharingPlatform)")
                .defaultHeader("Accept", "application/atom+xml,application/xml;q=0.9,*/*;q=0.8")
                .baseUrl("https://export.arxiv.org")
                .build(); // arXiv API 的 WebClient，设置 UA 与 Accept header
        this.achievementClient = achievementClient; // 保存注入的成就客户端引用
        this.fileServiceClient = fileServiceClient; // 保存注入的文件客户端引用
        // load persisted state if present
        loadProgress();
        loadProcessedIds();
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
            // 对每个配置的 OpenAlex 领域 ID 执行拉取，每个领域拉取 `perCategoryCount` 条记录
            String[] fieldIds = openAlexFieldIds.split("\\s*,\\s*");
            for (String fieldId : fieldIds) {
                try {
                    int page = progressMap.getOrDefault(fieldId, 1);
                    log.info("Fetching OpenAlex works for field {} (page={}, per-page={})", fieldId, page, perCategoryCount);
                    String body = openAlexClient.get()
                            .uri(uriBuilder -> uriBuilder.path("/works")
                            .queryParam("filter", "concepts.id:" + fieldId + ",is_oa:true")
                            .queryParam("per-page", String.valueOf(perCategoryCount))
                            .queryParam("page", String.valueOf(page))
                            .queryParam("sort", "cited_by_count:desc")
                            .build())
                            .retrieve()
                            .bodyToMono(String.class)
                            .block(Duration.ofSeconds(20)); // 阻塞等待响应

                    if (body == null) {
                        log.warn("OpenAlex returned empty body for field {}", fieldId);
                        continue;
                    }

                    JsonNode root = objectMapper.readTree(body);
                    JsonNode results = root.get("results");
                    if (results == null || !results.isArray()) {
                        log.warn("Unexpected OpenAlex response structure for field {}", fieldId);
                        continue;
                    }

                    Iterator<JsonNode> it = results.elements();
                    while (it.hasNext()) {
                        JsonNode work = it.next();
                        String title = work.path("title").asText("untitled");
                        String openalexId = work.path("id").asText();
                        String doi = work.path("doi").asText(null);

                        // dedup identifier: prefer doi, then arxiv id, then title hash
                        String identifier = null;
                        if (doi != null && !doi.isEmpty()) {
                            identifier = doi.toLowerCase();
                        }
                        if (identifier == null) {
                            JsonNode ids = work.path("ids");
                            if (!ids.isMissingNode() && ids.has("arxiv")) {
                                identifier = ids.path("arxiv").asText(null);
                            }
                        }
                        if (identifier == null) {
                            String t = work.path("title").asText(null);
                            if (t != null) {
                                identifier = "title:" + Integer.toHexString(t.hashCode());
                            }
                        }
                        if (identifier != null && processedIds.contains(identifier)) {
                            log.debug("Skipping already processed work {}", identifier);
                            continue;
                        }

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
                                            List<String> categories = new ArrayList<>();
                                            categories.add(domainMap.get(fieldId));
                                            String achJson = buildAchievementJson(title, authors, abstractText, finalFileId, null, categories);
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

                        if (identifier != null) {
                            processedIds.add(identifier);
                        }
                    }
                    // persist progress & processed ids after each field page
                    progressMap.put(fieldId, progressMap.getOrDefault(fieldId, 1) + 1);
                    saveProgress();
                    saveProcessedIds();
                } catch (Exception e) {
                    log.warn("OpenAlex fetch/upload phase failed for field {}: {}", fieldId, e.getMessage());
                }
            }

            // OpenAlex 部分处理完成，接着尝试从 arXiv 拉取记录（按每个领域拉取 `perCategoryCount` 条）
            int arxivCount = perCategoryCount > 0 ? perCategoryCount : 10;
            // Use top-level arXiv categories. If autoCategories was provided, use it as a shortcut list; otherwise use full default list.
            String[] cats;
            if (autoCategories != null && !autoCategories.isEmpty()) {
                cats = autoCategories.split("\\s*,\\s*");
            } else {
                cats = new String[]{"astro-ph", "cond-mat", "cs", "econ", "eess", "gr-qc", "hep-ex", "hep-lat", "hep-ph", "hep-th", "math", "math-ph", "nlin", "nucl-ex", "nucl-th", "physics", "q-bio", "q-fin", "quant-ph", "stat"};
            }
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

                        // dedup for arXiv entries: prefer arXiv id, then title hash
                        String identifier = null;
                        if (arxId != null && !arxId.isEmpty()) {
                            identifier = arxId;
                        }
                        if (identifier == null) {
                            String t2 = title;
                            if (t2 != null) {
                                identifier = "title:" + Integer.toHexString(t2.hashCode());
                            }
                        }
                        if (identifier != null && processedIds.contains(identifier)) {
                            log.debug("Skipping already processed arXiv entry {}", identifier);
                            continue;
                        }

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
                                            List<String> categories = new ArrayList<>();
                                            categories.add(domainMap.get(cat));
                                            String achJson = buildAchievementJson(title, authors, abstractText, finalFileId, null, categories);
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

                        // mark this arXiv entry as processed and persist
                        if (identifier != null) {
                            processedIds.add(identifier);
                            saveProcessedIds();
                        }
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
    private String buildAchievementJson(String title, java.util.List<String> authors, String abstractText, String fileId, Integer type, List<String> categories) {
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

            if (categories != null) {
                com.fasterxml.jackson.databind.node.ArrayNode arr = node.putArray("categories");
                for (String c : categories) {
                    arr.add(c);
                }
            } else {
                node.putArray("categories");
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

    // Persistence helpers for progress and processed ids
    private void loadProgress() {
        try {
            if (Files.exists(progressFile)) {
                Map<String, Integer> m = objectMapper.readValue(progressFile.toFile(), objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Integer.class));
                if (m != null) {
                    progressMap.putAll(m);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to load progress file: {}", e.getMessage());
        }
    }

    private void saveProgress() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(progressFile.toFile(), progressMap);
        } catch (IOException e) {
            log.warn("Failed to save progress file: {}", e.getMessage());
        }
    }

    private void loadProcessedIds() {
        try {
            if (Files.exists(processedFile)) {
                java.util.List<String> list = objectMapper.readValue(processedFile.toFile(), objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, String.class));
                if (list != null) {
                    processedIds.addAll(list);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to load processed ids file: {}", e.getMessage());
        }
    }

    private void saveProcessedIds() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(processedFile.toFile(), new java.util.ArrayList<>(processedIds));
        } catch (IOException e) {
            log.warn("Failed to save processed ids file: {}", e.getMessage());
        }
    }
}
