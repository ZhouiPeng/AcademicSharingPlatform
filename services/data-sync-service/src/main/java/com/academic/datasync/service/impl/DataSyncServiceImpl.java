// 包声明：当前类实现 DataSync 服务的逻辑
package com.academic.datasync.service.impl;

// 用于从 URL 提取文件名
import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.academic.datasync.client.AchievementServiceClient;
import com.academic.datasync.client.FileServiceClient;
import com.academic.datasync.service.DataSyncService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DataSyncServiceImpl implements DataSyncService {

    // 日志记录器
    private static final Logger log = LoggerFactory.getLogger(DataSyncServiceImpl.class);

    // 用于调用 OpenAlex 的 WebClient（只针对外部 API）
    private final WebClient webClient;
    // 用于调用 arXiv API（Atom feed）以解析/搜索 PDF 链接
    private final WebClient arxivClient;
    // JSON 解析器
    private final ObjectMapper objectMapper = new ObjectMapper();
    // 注入的成就服务客户端
    private final AchievementServiceClient achievementClient;
    // 注入的文件服务客户端
    private final FileServiceClient fileServiceClient;

    // 构造函数：注入 WebClient.Builder 与两个客户端
    public DataSyncServiceImpl(WebClient.Builder builder,
            AchievementServiceClient achievementClient,
            FileServiceClient fileServiceClient) {
        // 为 OpenAlex 创建单独的 WebClient 实例，增加内存缓冲以支持较大的响应体（5 MiB）
        org.springframework.web.reactive.function.client.ExchangeStrategies strategies
                = org.springframework.web.reactive.function.client.ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(5 * 1024 * 1024))
                        .build();
        this.webClient = builder.exchangeStrategies(strategies).baseUrl("https://api.openalex.org").build();
        this.arxivClient = org.springframework.web.reactive.function.client.WebClient.builder()
            .exchangeStrategies(strategies)
            .defaultHeader("User-Agent", "AcademicSharingPlatform/0.1 (github:AcademicSharingPlatform)")
            .defaultHeader("Accept", "application/atom+xml,application/xml;q=0.9,*/*;q=0.8")
            .baseUrl("https://export.arxiv.org")
            .build();
        this.achievementClient = achievementClient;
        this.fileServiceClient = fileServiceClient;
    }

    @Override
    public void pullFromPublicDb() {
        // 开始一次演示性的拉取并处理流程
        log.info("Starting pullFromPublicDb: fetching works from OpenAlex (demo limited)");
        try {
            // 请求 OpenAlex /works，增加每页数量以提高找到可下载 PDF 的概率（演示用途）
            // 将每页请求数量设置为 100（OpenAlex per-page 最大为 200）
            String body = webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/works")
                    .queryParam("filter", "is_oa:true")
                    .queryParam("per-page", "20")
                    .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(10));

            // 若响应为空则直接返回
            if (body == null) {
                log.warn("OpenAlex returned empty body");
                return;
            }

            // 解析 JSON 并获取 results 数组
            JsonNode root = objectMapper.readTree(body);
            JsonNode results = root.get("results");
            if (results == null || !results.isArray()) {
                log.warn("Unexpected OpenAlex response structure");
                return;
            }

            // 遍历每一条 work（论文元数据）
            Iterator<JsonNode> it = results.elements();
            while (it.hasNext()) {
                JsonNode work = it.next();
                // 读取标题、OpenAlex id、DOI
                String title = work.path("title").asText("untitled");
                String openalexId = work.path("id").asText();
                String doi = work.path("doi").asText(null);

                String pdfUrl = extractPdfUrl(work);

                // 生成回退的 fileId
                String fallbackFileId = openalexId.isEmpty() ? (doi != null ? doi : "oa-" + System.currentTimeMillis()) : openalexId;
                String finalFileId = fallbackFileId;

                // 若存在 PDF，则尝试下载并上传到文件服务，成功则使用文件服务返回的 fileId
                if (pdfUrl != null && !pdfUrl.isEmpty()) {
                    try {
                        log.info("Found PDF URL for work {}: {}", openalexId, pdfUrl);

                        // 尝试从 URL 提取文件名（回退到 finalFileId.pdf）
                        // Use the paper title as filename (sanitized). Fallback to finalFileId if title missing.
                        String rawTitle = title == null || title.isEmpty() ? finalFileId : title;
                        String filename = sanitizeFilename(rawTitle);

                        // Use client helper to stream-download and upload without writing to disk
                        String uploadResp = fileServiceClient.uploadFromUrl("datasync", pdfUrl, filename);
                        if (uploadResp != null) {
                            try {
                                JsonNode uploadRoot = objectMapper.readTree(uploadResp);
                                JsonNode data = uploadRoot.path("data");
                                String uploadedFileId = data.path("fileId").asText(null);
                                if (uploadedFileId != null && !uploadedFileId.isEmpty()) {
                                    finalFileId = uploadedFileId;
                                    log.info("Uploaded PDF for work {} to file-service, fileId={}", openalexId, finalFileId);
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

                // 构造成就上报的最小负载
                Map<String, Object> achPayload = new HashMap<>();
                achPayload.put("userId", "datasync");
                achPayload.put("title", title);
                achPayload.put("fileId", finalFileId);

                // 序列化并上报到成就服务
                String jsonPayload = objectMapper.writeValueAsString(achPayload);
                // 临时：跳过对成就服务的实际调用（该服务尚未就绪），以便在本地仅测试拉取/上传流程
                log.info("[TEST MODE] Skipping achievement service call for work {} -> payload={}", openalexId, jsonPayload);
            }

            // 整个演示流程完成
            // 完成 OpenAlex 部分后，再从 arXiv 拉取相同数量的记录
            int arxivCount = 20; // 与 OpenAlex 请求数对应
            try {
                // Use a sensible default query; change to desired query. Examples:
                // "cat:cs.AI" (category), "all:machine learning" (keyword),
                // or "au:del_maestro+AND+ti:checkerboard" (author+title).
                final String ARXIV_SEARCH_QUERY = "cat:cs.AI"; // default to CS.AI category
                String encoded = encodeArxivQuery(ARXIV_SEARCH_QUERY);
                String path = "/api/query?search_query=" + encoded + "&max_results=" + arxivCount;
                log.debug("arXiv request path: {}", path);
                String arxivBody = arxivClient.get()
                        .uri(path)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block(Duration.ofSeconds(10));
                log.debug("arXiv response length: {}", arxivBody == null ? 0 : arxivBody.length());

                if (arxivBody == null) {
                    log.warn("arXiv returned empty body for search {}", ARXIV_SEARCH_QUERY);
                } else {
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

            log.info("pullFromPublicDb finished (demo run)");
        } catch (Exception e) {
            // 捕获并记录异常
            log.error("pullFromPublicDb failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Find a work on OpenAlex by arXiv id and upload its PDF via file-service
     * (streamed). Returns the file-service response body (JSON) or null on
     * failure.
     */
    public String uploadWorkFromOpenAlexByArxiv(String arxivId) {
        try {
            String body = webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/works")
                    .queryParam("filter", "ids.arxiv:" + arxivId)
                    .queryParam("per-page", "1")
                    .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(10));

            if (body == null) {
                log.warn("OpenAlex returned empty body for arXiv {}", arxivId);
                return null;
            }

            JsonNode root = objectMapper.readTree(body);
            JsonNode results = root.path("results");
            if (!results.isArray() || results.size() == 0) {
                log.warn("No OpenAlex work found for arXiv {}", arxivId);
                return null;
            }

            JsonNode work = results.get(0);
            String title = work.path("title").asText("untitled");
            String pdfUrl = extractPdfUrl(work);
            if (pdfUrl == null) {
                log.warn("No PDF URL found for arXiv {}", arxivId);
                return null;
            }

            String filename = sanitizeFilename(title == null || title.isEmpty() ? arxivId : title);
            log.info("Uploading from OpenAlex arXiv {} -> url={} filename={}", arxivId, pdfUrl, filename);
            return fileServiceClient.uploadFromUrl("datasync", pdfUrl, filename);

        } catch (Exception e) {
            log.error("uploadWorkFromOpenAlexByArxiv failed for {}: {}", arxivId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Deterministic helper for tests: given an arXiv id, try to resolve a PDF
     * URL (via arXiv API lookup) and upload it to file-service. Returns the
     * raw file-service response body (JSON) or null on failure.
     */
    public String uploadFromArxivById(String arxivId) {
        if (arxivId == null || arxivId.isEmpty()) return null;
        try {
            String pdfUrl = lookupArxivPdfUrl(arxivId);
            if (pdfUrl == null) {
                pdfUrl = arxivPdfFromId(arxivId);
            }
            if (pdfUrl == null) return null;

            String filename = sanitizeFilename(arxivId + ".pdf");
            log.info("Uploading arXiv {} -> {}", arxivId, pdfUrl);
            return fileServiceClient.uploadFromUrl("datasync", pdfUrl, filename);
        } catch (Exception e) {
            log.error("uploadFromArxivById failed for {}: {}", arxivId, e.getMessage(), e);
            return null;
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
    private String extractPdfUrl(JsonNode work) {
        if (work == null || work.isMissingNode()) {
            return null;
        }

        // 1) best_oa_location
        JsonNode best = work.path("best_oa_location");
        if (!best.isMissingNode()) {
            String u = textOrNull(best, "url_for_pdf");
            if (isPdfUrl(u)) {
                return u;
            }
            u = textOrNull(best, "url");
            if (isPdfUrl(u)) {
                return u;
            }
        }

        // 2) primary_location
        JsonNode primary = work.path("primary_location");
        if (!primary.isMissingNode()) {
            String u = textOrNull(primary, "url_for_pdf");
            if (isPdfUrl(u)) {
                return u;
            }
            u = textOrNull(primary, "url");
            if (isPdfUrl(u)) {
                return u;
            }
        }

        // 2b) try oa_locations array (many OpenAlex records list multiple OA locations)
        JsonNode oaLocations = work.path("oa_locations");
        if (oaLocations != null && oaLocations.isArray()) {
            for (JsonNode loc : oaLocations) {
                String u = textOrNull(loc, "url_for_pdf");
                if (isPdfUrl(u)) {
                    return u;
                }
                u = textOrNull(loc, "url");
                if (isPdfUrl(u)) {
                    return u;
                }
            }
        }

        // 3) oa_url field (some datasets provide this)
        String oa = textOrNull(work, "oa_url");
        if (isPdfUrl(oa)) {
            return oa;
        }

        // 4) ids -> arXiv id
        JsonNode ids = work.path("ids");
        if (!ids.isMissingNode()) {
            // OpenAlex may include an 'arxiv' key or an array/object; try to find arXiv id
            if (ids.has("arxiv")) {
                String arx = ids.path("arxiv").asText(null);
                if (arx != null && !arx.isEmpty()) {
                    return arxivPdfFromId(arx);
                }
            }
            // Fallback: iterate fields under ids to find any value that looks like arXiv
            Iterator<String> idFields = ids.fieldNames();
            while (idFields.hasNext()) {
                String key = idFields.next();
                String v = ids.path(key).asText(null);
                if (v != null && looksLikeArXivId(v)) {
                    return arxivPdfFromId(v);
                }
            }
        }

        // 5) Some works include a top-level 'id' or URLs; try to scan common url fields
        String[] candidates = {textOrNull(work, "url"), textOrNull(work, "id"), textOrNull(work, "uri")};
        for (String c : candidates) {
            if (isPdfUrl(c)) {
                return c;
            }
            // if it's an arXiv abs URL, convert to pdf
            if (c != null && c.contains("arxiv.org/abs/")) {
                String arx = c.substring(c.lastIndexOf('/') + 1);
                return arxivPdfFromId(arx);
            }
        }

        return null;
    }

    private String textOrNull(JsonNode node, String field) {
        if (node == null || node.isMissingNode()) {
            return null;
        }
        String v = node.path(field).asText(null);
        return (v == null || v.isEmpty()) ? null : v;
    }

    private boolean isPdfUrl(String u) {
        if (u == null) {
            return false;
        }
        String lower = u.toLowerCase();
        return lower.endsWith(".pdf") || lower.contains("/pdf") || lower.contains("arxiv.org/pdf") || (lower.contains("drive.google.com") && lower.contains("export=download"));
    }

    private boolean looksLikeArXivId(String s) {
        if (s == null) {
            return false;
        }
        // basic patterns: 1706.03762 or arXiv:1706.03762 or abs/1706.03762
        return s.matches("(?i)(arxiv:)?\\d{4}\\.\\d{4,5}(|v\\d+)") || s.toLowerCase().contains("arxiv");
    }

    private String arxivPdfFromId(String arx) {
        if (arx == null) {
            return null;
        }
        // Normalize forms like 'arXiv:1706.03762' -> '1706.03762'
        arx = arx.replaceAll("(?i)arxiv:", "").trim();
        // If contains slash (like 'abs/1706.03762'), take trailing part
        if (arx.contains("/")) {
            arx = arx.substring(arx.lastIndexOf('/') + 1);
        }
        return "https://arxiv.org/pdf/" + arx + ".pdf";
    }

    /**
     * Query arXiv API (Atom) for the given arXiv id and try to extract a PDF URL.
     * Returns PDF href if found, otherwise null.
     */
    private String lookupArxivPdfUrl(String arx) {
        if (arx == null || arx.isEmpty()) {
            return null;
        }
        try {
            String body = arxivClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/query").queryParam("id_list", arx).build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(10));

            if (body == null) {
                return null;
            }

            Document doc = parseXml(body);
            if (doc == null) return null;
            NodeList entries = doc.getElementsByTagName("entry");
            for (int i = 0; i < entries.getLength(); i++) {
                Element entry = (Element) entries.item(i);
                NodeList links = entry.getElementsByTagName("link");
                for (int j = 0; j < links.getLength(); j++) {
                    Element link = (Element) links.item(j);
                    String href = link.getAttribute("href");
                    String title = link.getAttribute("title");
                    String type = link.getAttribute("type");
                    if ((title != null && title.equalsIgnoreCase("pdf")) || (type != null && type.toLowerCase().contains("pdf")) || (href != null && href.contains("/pdf/"))) {
                        return href;
                    }
                }
            }

        } catch (Exception e) {
            log.debug("lookupArxivPdfUrl error for {}: {}", arx, e.getMessage());
        }
        return null;
    }

    private Document parseXml(String xml) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(xml));
            return db.parse(is);
        } catch (Exception e) {
            log.debug("parseXml failed: {}", e.getMessage());
            return null;
        }
    }

    private String encodeArxivQuery(String q) {
        if (q == null) return "";
        return URLEncoder.encode(q, StandardCharsets.UTF_8);
    }

    /**
     * Sanitize a title to a safe filename. Replaces invalid characters with
     * underscores and ensures a .pdf extension.
     */
    private String sanitizeFilename(String title) {
        if (title == null) {
            return "untitled.pdf";
        }
        String s = title.trim();
        // Replace path separators and control chars
        s = s.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]+", "_");
        // Collapse whitespace
        s = s.replaceAll("\\s+", "_");
        if (!s.toLowerCase().endsWith(".pdf")) {
            s = s + ".pdf";
        }
        // Limit length
        if (s.length() > 200) {
            s = s.substring(0, 200);
        }
        return s;
    }
}
