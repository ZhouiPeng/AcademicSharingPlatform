package com.academic.datasync.service.impl; // 定义包名

// 引入需要的类与接口，下面每行都附带中文注释以说明用途
import java.io.IOException; // 用于把字符串包装为 Reader 以便 XML 解析
import java.nio.file.Files; // 提供 HashMap 实现
import java.nio.file.Path; // 用于遍历集合的迭代器
import java.nio.file.Paths; // Map 接口，用于构建上报负载
import java.time.Duration; // 正则匹配器
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map; // 正则模式
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger; // DOM 解析器的构建器
import org.slf4j.LoggerFactory; // DOM DocumentBuilder 的工厂
import org.springframework.beans.factory.annotation.Value; // 日志接口
import org.springframework.scheduling.annotation.Scheduled; // 用于注入配置开关与列表
import org.springframework.stereotype.Service; // 用于计划任务注解
import org.springframework.web.reactive.function.client.WebClient; // 日志工厂，用于创建 Logger

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import com.academic.datasync.client.AchievementServiceClient; // DOM NodeList 表示节点列表
import com.academic.datasync.client.FileServiceClient; // 将字符串包装为 InputSource 供解析器使用
import com.academic.datasync.service.DataSyncService; // 成就服务客户端接口（注入）
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode; // 文件服务客户端接口（注入）
import com.fasterxml.jackson.databind.ObjectMapper; // DataSync 服务接口

@Service // 声明这是一个 Spring 管理的服务组件
public class DataSyncServiceImpl implements DataSyncService { // 实现 DataSyncService 接口

    // 日志记录器，用于输出运行时信息
    private static final Logger log = LoggerFactory.getLogger(DataSyncServiceImpl.class); // 获取当前类的 Logger

    // WebClient 实例用于访问 OpenAlex API（元数据）
    private final WebClient openAlexClient; // 用于调用 OpenAlex 的客户端
    // JSON 解析器实例
    private final ObjectMapper objectMapper = new ObjectMapper(); // Jackson 的 ObjectMapper
    // 成就服务客户端（注入）
    private final AchievementServiceClient achievementClient; // 注入用于上报成就的客户端
    // 文件服务客户端（注入）
    private final FileServiceClient fileServiceClient; // 注入用于上传文件的客户端
    private final Map<String, String> domainMap = new HashMap<String, String>() {
        {
            put("C121332964", "物理学");
            put("C41008148", "计算机科学");
            put("C33923547", "数学");
            put("C144133960", "材料科学");
            put("C185592680", "化学");
            put("C86803240", "生物学");
            put("C127413603", "工程学");
            put("C16203183", "经济学");
            put("C106769008", "商业");
            put("C17773945", "政治学");
            put("C58743932", "社会学");
            put("C95457728", "心理学");
            put("C127961042", "哲学");
            put("C112351118", "历史学");
            put("C15744967", "艺术");
            put("C71924100", "医学");
            put("C39432304", "环境科学");
            put("C162324750", "地理学");
            put("C138885662", "地质学");
        }
    };

    // 自动爬取相关配置（从 application.yml 注入）
    @Value("${datasync.auto-enabled:false}")
    private boolean autoCrawlEnabled; // 开关：为 true 时启用定时爬取

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

    // Prevent concurrent runs in the same instance (manual trigger + scheduled trigger)
    private final AtomicBoolean crawlRunning = new AtomicBoolean(false);

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
        if (!crawlRunning.compareAndSet(false, true)) {
            log.warn("pullFromPublicDb skipped: another crawl is already running");
            return;
        }
        pullFromPublicDbMono()
                .doOnError(e -> log.error("Error: pullFromPublicDb reactive pipeline failed: {}", e.getMessage(), e))
                .doFinally(st -> crawlRunning.set(false))
                .subscribe();
    }

    private Mono<Void> pullFromPublicDbMono() {
        // 对每个配置的 OpenAlex 领域 ID 执行拉取，每个领域拉取 `perCategoryCount` 条记录
        String[] fieldIds = openAlexFieldIds.split("\\s*,\\s*");
        return Flux.fromArray(fieldIds)
                // 串行处理，避免并发写进度文件/processedIds
                .concatMap(this::processOpenAlexField)
                .then();
    }

    private Mono<Void> processOpenAlexField(String fieldId) {
        int page = progressMap.getOrDefault(fieldId, 1);
        log.info("Info: Fetching OpenAlex works for field {} (page={}, per-page={})", fieldId, page, perCategoryCount);

        Mono<String> bodyMono = openAlexClient.get()
                .uri(uriBuilder -> uriBuilder.path("/works")
                .queryParam("filter", "concepts.id:" + fieldId + ",is_oa:true")
                .queryParam("per-page", String.valueOf(perCategoryCount))
                .queryParam("page", String.valueOf(page))
                .queryParam("sort", "cited_by_count:desc")
                .build())
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(20));

        Mono<Void> persistProgressMono = Mono.fromCallable(() -> {
            // persist progress & processed ids after each field page
            progressMap.put(fieldId, progressMap.getOrDefault(fieldId, 1) + 1);
            saveProgress();
            saveProcessedIds();
            return (Void) null;
        }).subscribeOn(Schedulers.boundedElastic());

        Mono<Void> fetchAndProcessMono = bodyMono.flatMap(body -> {
            if (body == null) {
                log.warn("Warn: OpenAlex returned empty body for field {}", fieldId);
                return Mono.<Void>empty();
            }

            Mono<Void> workProcessingMono = Mono.fromCallable(() -> objectMapper.readTree(body))
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMapMany(root -> {
                        JsonNode results = root.get("results");
                        if (results == null || !results.isArray()) {
                            log.warn("Warn: Unexpected OpenAlex response structure for field {}", fieldId);
                            return Flux.<JsonNode>empty();
                        }

                        List<JsonNode> works = new ArrayList<>();
                        Iterator<JsonNode> it = results.elements();
                        while (it.hasNext()) {
                            works.add(it.next());
                        }
                        return Flux.fromIterable(works);
                    })
                    .concatMap(work -> processOneWork(fieldId, work))
                    .then();

            return workProcessingMono;
        });

        return fetchAndProcessMono
                .then(persistProgressMono)
                .onErrorResume(e -> {
                    log.warn("Warn: OpenAlex fetch/upload phase failed for field {}: {}", fieldId, e.getMessage());
                    return Mono.<Void>empty();
                });
    }

    private Mono<Void> processOneWork(String fieldId, JsonNode work) {
        return Mono.defer(() -> {
            String title = work.path("title").asText("untitled");
            String openalexId = work.path("id").asText();
            @SuppressWarnings("unused")
            String type = work.path("type").asText();

            // (1) Dedup key uses stable openalexId. If duplicated, skip BOTH file and achievement.
            if (openalexId == null || openalexId.isBlank()) {
                log.warn("Warn: OpenAlex work missing id; skipping work with title='{}'", title);
                return Mono.empty();
            }

            String identifier = "openalex:" + openalexId;
            if (processedIds.contains(identifier)) {
                log.debug("Debug: Skipping already processed work {}", identifier);
                return Mono.empty();
            }

            final String identifierFinal = identifier;

            String rawTitle = (title == null || title.isEmpty()) ? "untitled" : title;
            String filename = sanitizeFilename(rawTitle);

            Mono<String> uploadRespMono;
            uploadRespMono = Mono.fromCallable(() -> fileServiceClient.uploadFromUrl("datasync", openalexId, filename))
                    .subscribeOn(Schedulers.boundedElastic())
                    .onErrorResume(e -> {
                        log.warn("Warn: uploadFromUrl threw for work {}: {}", openalexId, e.getMessage());
                        return Mono.empty();
                    });

            return uploadRespMono
                    .flatMap(uploadResp -> Mono.fromCallable(() -> {
                if (uploadResp == null) {
                    return Optional.<String>empty();
                }
                JsonNode uploadRoot = objectMapper.readTree(uploadResp);
                JsonNode data = uploadRoot.path("data");
                String fileId = data.path("fileId").asText(null);
                return Optional.ofNullable(fileId);
            }).subscribeOn(Schedulers.boundedElastic())
                    .onErrorResume(pe -> {
                        log.warn("Warn: Failed to parse upload response for work {}: {}", openalexId, pe.getMessage());
                        return Mono.just(Optional.empty());
                    }))
                    .switchIfEmpty(Mono.just(Optional.empty()))
                    .flatMap(finalFileIdOpt -> {
                        if (finalFileIdOpt.isEmpty()) {
                            log.warn("Warn: File upload returned empty fileId for work {}.", openalexId);
                            return Mono.empty();
                        }

                        String finalFileId = finalFileIdOpt.get();
                        log.info("Info: Parsed fileId for work {} -> {}", openalexId, finalFileId);

                        List<String> authors = extractAuthorsFromWork(work);
                        String abstractText = textOrNull(work, "abstract");
                        List<String> categories = new ArrayList<>();
                        String domain = domainMap.get(fieldId);
                        if (domain != null) {
                            categories.add(domain);
                        }
                        String achJson = buildAchievementJson(title, null, abstractText, finalFileId, null, categories);
                        if (achJson == null || achJson.isBlank()) {
                            log.warn("Warn: buildAchievementJson returned empty payload for work {} (fileId={})", openalexId, finalFileId);
                            return Mono.empty();
                        }
                        log.info(
                                "Info: Calling achievement-service for work {} (fileId={}, titleLen={}, abstractLen={}, categories={})",
                                openalexId,
                                finalFileId,
                                title == null ? 0 : title.length(),
                                abstractText == null ? 0 : abstractText.length(),
                                categories == null ? 0 : categories.size());

                        return achievementClient.createAchievement(achJson)
                                .doOnNext(achId -> log.info("Info: achievement-service created achievement for work {} -> achievementId={}", openalexId, achId))
                                .doOnError(ex -> log.warn("Warn: createAchievement threw for work {} (fileId={}): {}", openalexId, finalFileId, ex.getMessage()))
                                // preserve overall pipeline behavior: don't crash the whole crawl
                                .onErrorResume(ex -> Mono.empty())
                                .flatMap(achId -> Mono.fromCallable(() -> {
                            if (identifierFinal != null) {
                                processedIds.add(identifierFinal);
                            }
                            return (Void) null;
                        }).subscribeOn(Schedulers.boundedElastic()))
                                .switchIfEmpty(Mono.fromRunnable(() -> log.warn(
                                "Warn: achievement-service returned empty achievementId for work {} (fileId={}); NOT marking processed, will retry next run",
                                openalexId,
                                finalFileId)));
                    });
        });
    }

    private String textOrNull(JsonNode node, String field) { // 安全读取字段值，若为空则返回 null
        if (node == null || node.isMissingNode()) {
            return null; // 空节点返回 null
        }
        String v = node.path(field).asText(null); // 直接尝试读取字段
        return (v == null || v.isEmpty()) ? null : v; // 空串返回 null
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

    // 构造符合 AchievementDto 的 JSON 字符串
    private String buildAchievementJson(String title, java.util.List<String> authors, String abstractText, String fileId, String type, List<String> categories) {
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
        } catch (JsonProcessingException e) {
            log.warn("buildAchievementJson failed: {}", e.getMessage());
            return null;
        }
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
