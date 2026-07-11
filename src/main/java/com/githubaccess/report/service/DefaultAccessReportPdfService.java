package com.githubaccess.report.service;

import com.githubaccess.report.dto.AccessReportResponse;
import org.springframework.stereotype.Service;

@Service
public class DefaultAccessReportPdfService implements AccessReportPdfService {

    private final AccessReportService accessReportService;
    private final AccessReportPdfGenerator accessReportPdfGenerator;

    public DefaultAccessReportPdfService(AccessReportService accessReportService,
                                         AccessReportPdfGenerator accessReportPdfGenerator) {
        this.accessReportService = accessReportService;
        this.accessReportPdfGenerator = accessReportPdfGenerator;
    }

    @Override
    public byte[] generateAccessReportPdf(String organization) {
        AccessReportResponse report = accessReportService.generateAccessReport(organization);
        return accessReportPdfGenerator.generate(report);
    }
}
