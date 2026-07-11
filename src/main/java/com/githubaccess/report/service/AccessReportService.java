package com.githubaccess.report.service;

import com.githubaccess.report.dto.AccessReportResponse;

public interface AccessReportService {

    AccessReportResponse generateAccessReport(String organization);
}
