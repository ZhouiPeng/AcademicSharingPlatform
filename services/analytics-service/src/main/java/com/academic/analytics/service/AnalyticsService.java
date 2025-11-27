package com.academic.analytics.service;

import com.academic.analytics.dto.*;

public interface AnalyticsService {

    HotTopicsResponse hotTopics(HotTopicsRequest request);

    ReportExportResponse getReport(String reportId, ReportExportRequest request);

    AchievementsStatsResponse achievementsStats(AchievementsStatsRequest request);
}
