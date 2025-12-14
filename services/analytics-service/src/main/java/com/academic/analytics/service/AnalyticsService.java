package com.academic.analytics.service;

import com.academic.analytics.dto.AchievementsStatsRequest;
import com.academic.analytics.dto.AchievementsStatsResponse;
import com.academic.analytics.dto.HotTopicsRequest;
import com.academic.analytics.dto.HotTopicsResponse;
import com.academic.analytics.dto.ReportExportRequest;
import com.academic.analytics.dto.ReportExportResponse;

public interface AnalyticsService {

    HotTopicsResponse hotTopics(HotTopicsRequest request);

    ReportExportResponse getReport(String reportId, ReportExportRequest request);

    AchievementsStatsResponse achievementsStats(AchievementsStatsRequest request);

    // record a search term (term must be non-null/non-empty)
    void collectSearchTerm(String term);

    // collect or merge author relationship for a user
    void collectAuthorRelationship(String userId, String authors);

    // return merged authors string for a userId (may be empty)
    String getAuthorRelationship(String userId);
}
