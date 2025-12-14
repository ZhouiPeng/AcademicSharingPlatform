package com.academic.analytics.service.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academic.analytics.dto.AchievementsStatsRequest;
import com.academic.analytics.dto.AchievementsStatsResponse;
import com.academic.analytics.dto.HotTopicItem;
import com.academic.analytics.dto.HotTopicsRequest;
import com.academic.analytics.dto.HotTopicsResponse;
import com.academic.analytics.dto.ReportExportRequest;
import com.academic.analytics.dto.ReportExportResponse;
import com.academic.analytics.dto.TypeDistributionItem;
import com.academic.analytics.dto.YearlyGrowthItem;
import com.academic.analytics.entity.AuthorRelationshipEntity;
import com.academic.analytics.entity.SearchTermEntity;
import com.academic.analytics.repository.AuthorRelationshipRepository;
import com.academic.analytics.repository.SearchTermRepository;
import com.academic.analytics.service.AnalyticsService;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private final SearchTermRepository searchTermRepository;
    private final AuthorRelationshipRepository authorRelationshipRepository;

    public AnalyticsServiceImpl(SearchTermRepository searchTermRepository, AuthorRelationshipRepository authorRelationshipRepository) {
        this.searchTermRepository = searchTermRepository;
        this.authorRelationshipRepository = authorRelationshipRepository;
    }

    @Override
    public HotTopicsResponse hotTopics(HotTopicsRequest request) {
        HotTopicsResponse resp = new HotTopicsResponse();
        resp.setReportId("rep-" + System.currentTimeMillis());

        // aggregate top terms from DB
        List<SearchTermEntity> all = searchTermRepository.findAll();
        Map<String, Long> top = all.stream().collect(Collectors.toMap(SearchTermEntity::getTerm, SearchTermEntity::getCount));
        List<HotTopicItem> items = top.entrySet().stream()
                .sorted(Comparator.comparingLong(Map.Entry<String, Long>::getValue).reversed())
                .limit(20)
                .map(e -> {
                    HotTopicItem it = new HotTopicItem();
                    it.setKeyword(e.getKey());
                    it.setCount(e.getValue().intValue());
                    return it;
                }).collect(Collectors.toList());

        resp.setHotTopics(items);
        resp.setEmergingDirs(List.of("AI+教育", "AI+医疗"));
        return resp;
    }

    @Override
    public ReportExportResponse getReport(String reportId, ReportExportRequest request) {
        ReportExportResponse r = new ReportExportResponse();
        String ext = (request != null && request.getReportFormat() != null && request.getReportFormat() == 2) ? "xlsx" : "pdf";
        r.setReportUrl("https://obs-academic.xxx.com/static/reports/" + reportId + "." + ext + "?sign=stub");
        r.setExpireTime("2099-12-31 23:59:59");
        return r;
    }

    @Override
    public AchievementsStatsResponse achievementsStats(AchievementsStatsRequest request) {
        AchievementsStatsResponse r = new AchievementsStatsResponse();
        r.setTotalCount(5000);
        List<TypeDistributionItem> dist = new ArrayList<>();
        dist.add(new TypeDistributionItem(1, "期刊论文", 3000, 60));
        dist.add(new TypeDistributionItem(2, "专利", 1000, 20));
        r.setTypeDistribution(dist);
        List<YearlyGrowthItem> yearly = new ArrayList<>();
        yearly.add(new YearlyGrowthItem(2024, 5000, 15));
        r.setYearlyGrowth(yearly);
        return r;
    }

    @Override
    @Transactional
    public void collectSearchTerm(String term) {
        if (term == null) {
            return;
        }
        String t = term.trim();
        if (t.isEmpty()) {
            return;
        }
        String key = t;
        long now = Instant.now().toEpochMilli();
        SearchTermEntity e = searchTermRepository.findById(key).orElse(null);
        if (e == null) {
            e = new SearchTermEntity(key, 1L, now);
        } else {
            Long c = e.getCount() == null ? 0L : e.getCount();
            e.setCount(c + 1);
            e.setLastSeen(now);
        }
        searchTermRepository.save(e);
    }

    @Override
    @Transactional
    public void collectAuthorRelationship(String userId, String authors) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        String newAuthors = authors == null ? "" : authors.trim();
        java.util.Optional<AuthorRelationshipEntity> opt = authorRelationshipRepository.findById(userId);
        if (opt.isEmpty()) {
            AuthorRelationshipEntity entity = new AuthorRelationshipEntity(userId, newAuthors);
            authorRelationshipRepository.save(entity);
            return;
        }
        AuthorRelationshipEntity existing = opt.get();
        String existingAuthors = existing.getAuthors() == null ? "" : existing.getAuthors();

        java.util.Set<String> merged = new java.util.LinkedHashSet<>();
        java.util.stream.Stream.of(existingAuthors.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(merged::add);
        java.util.stream.Stream.of(newAuthors.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(merged::add);

        String joined = String.join(",", merged);
        existing.setAuthors(joined);
        authorRelationshipRepository.save(existing);
    }

    @Override
    public String getAuthorRelationship(String userId) {
        if (userId == null || userId.isBlank()) {
            return "";
        }
        return authorRelationshipRepository.findById(userId)
                .map(e -> e.getAuthors() == null ? "" : e.getAuthors())
                .orElse("");
    }
}
