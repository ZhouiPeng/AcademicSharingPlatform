package com.academic.analytics.dto;

public class ReportExportRequest {

    private Integer reportFormat; // 1=PDF,2=Excel

    public Integer getReportFormat() {
        return reportFormat;
    }

    public void setReportFormat(Integer reportFormat) {
        this.reportFormat = reportFormat;
    }
}
