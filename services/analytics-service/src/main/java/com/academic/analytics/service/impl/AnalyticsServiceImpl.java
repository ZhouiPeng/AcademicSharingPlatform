package com.academic.analytics.service.impl;

import com.academic.analytics.dto.*;
import com.academic.analytics.service.AnalyticsService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    @Override
    public HotTopicsResponse hotTopics(HotTopicsRequest request) {
        HotTopicsResponse resp = new HotTopicsResponse();
        resp.setReportId("rep-" + System.currentTimeMillis());

        List<HotTopicItem> items = new ArrayList<>();
        HotTopicItem it = new HotTopicItem();
        it.setKeyword("生成式AI");
        it.setCount(1200);
        if (request != null && request.getAnalysisDim() != null && request.getAnalysisDim() == 2) {
            List<Integer> trend = List.of(100, 150, 200, 250, 300, 200);
            it.setTrendData(trend);
        }
        items.add(it);
        resp.setHotTopics(items);
        resp.setEmergingDirs(List.of("AI+教育", "AI+医疗"));
        return resp;
    }

    @Override
    public ReportExportResponse getReport(String reportId, ReportExportRequest request) {
        ReportExportResponse r = new ReportExportResponse();
        // stubbed URL - in production this would be generated from object storage
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
}
