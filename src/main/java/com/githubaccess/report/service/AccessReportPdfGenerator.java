package com.githubaccess.report.service;

import com.githubaccess.report.dto.AccessPermissionsDto;
import com.githubaccess.report.dto.AccessReportResponse;
import com.githubaccess.report.dto.RepositoryAccessDto;
import com.githubaccess.report.dto.UserAccessDto;
import com.githubaccess.report.exception.TechnicalException;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class AccessReportPdfGenerator {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);

    private static final Color HEADER_BG = new Color(31, 41, 55);
    private static final Color ACCENT = new Color(37, 99, 235);
    private static final Color ROW_ALT = new Color(243, 244, 246);
    private static final Color BORDER = new Color(209, 213, 219);
    private static final Color TEXT_MUTED = new Color(75, 85, 99);

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.WHITE);
    private static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.WHITE);
    private static final Font SECTION_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, HEADER_BG);
    private static final Font BODY_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
    private static final Font BODY_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
    private static final Font SMALL_MUTED = FontFactory.getFont(FontFactory.HELVETICA, 9, TEXT_MUTED);
    private static final Font TABLE_HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
    private static final Font TABLE_CELL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);

    public byte[] generate(AccessReportResponse report) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 40, 40, 50, 50);
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            writer.setPageEvent(new PageNumberFooter(report.organization()));
            document.open();

            addCoverHeader(document, report);
            addSummaryCards(document, report);
            addLegend(document);
            addUserSections(document, report.users());

            document.close();
            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw new TechnicalException("Failed to generate access report PDF", ex);
        }
    }

    private void addCoverHeader(Document document, AccessReportResponse report) throws DocumentException {
        PdfPTable banner = new PdfPTable(1);
        banner.setWidthPercentage(100);
        banner.setSpacingAfter(16);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(HEADER_BG);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(16);

        Paragraph title = new Paragraph("GitHub Organization Access Report", TITLE_FONT);
        title.setSpacingAfter(6);
        cell.addElement(title);

        Paragraph orgLine = new Paragraph("Organization: " + report.organization(), SUBTITLE_FONT);
        orgLine.setSpacingAfter(4);
        cell.addElement(orgLine);

        Paragraph generated = new Paragraph(
                "Generated: " + TIMESTAMP_FORMAT.format(report.generatedAt()),
                SUBTITLE_FONT);
        cell.addElement(generated);

        banner.addCell(cell);
        document.add(banner);
    }

    private void addSummaryCards(Document document, AccessReportResponse report) throws DocumentException {
        Paragraph summaryTitle = new Paragraph("Summary", SECTION_FONT);
        summaryTitle.setSpacingAfter(8);
        document.add(summaryTitle);

        PdfPTable summary = new PdfPTable(3);
        summary.setWidthPercentage(100);
        summary.setSpacingAfter(16);
        summary.setWidths(new float[]{1f, 1f, 1f});

        summary.addCell(summaryCard("Users with access", String.valueOf(report.totalUsers())));
        summary.addCell(summaryCard("Repositories scanned", String.valueOf(report.totalRepositories())));
        summary.addCell(summaryCard("Access mappings", String.valueOf(countMappings(report))));

        document.add(summary);
    }

    private PdfPCell summaryCard(String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(BORDER);
        cell.setBorderWidth(1f);
        cell.setPadding(12);
        cell.setBackgroundColor(Color.WHITE);

        Paragraph valuePara = new Paragraph(value, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, ACCENT));
        valuePara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(valuePara);

        Paragraph labelPara = new Paragraph(label, SMALL_MUTED);
        labelPara.setAlignment(Element.ALIGN_CENTER);
        labelPara.setSpacingBefore(4);
        cell.addElement(labelPara);
        return cell;
    }

    private void addLegend(Document document) throws DocumentException {
        Paragraph legendTitle = new Paragraph("Permission levels", SECTION_FONT);
        legendTitle.setSpacingAfter(6);
        document.add(legendTitle);

        Paragraph legend = new Paragraph(
                "admin — full control  |  maintain — manage without security settings  |  "
                        + "write — push code  |  triage — manage issues/PRs  |  read — view and clone",
                SMALL_MUTED);
        legend.setSpacingAfter(18);
        document.add(legend);
    }

    private void addUserSections(Document document, List<UserAccessDto> users) throws DocumentException {
        Paragraph usersTitle = new Paragraph("User access details", SECTION_FONT);
        usersTitle.setSpacingAfter(10);
        document.add(usersTitle);

        if (users == null || users.isEmpty()) {
            document.add(new Paragraph("No users with repository access were found.", BODY_FONT));
            return;
        }

        int index = 1;
        for (UserAccessDto user : users) {
            addUserBlock(document, user, index++);
        }
    }

    private void addUserBlock(Document document, UserAccessDto user, int index) throws DocumentException {
        Paragraph userHeader = new Paragraph();
        userHeader.add(new Chunk(index + ". ", BODY_BOLD));
        userHeader.add(new Chunk(user.login(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, ACCENT)));
        userHeader.add(new Chunk(
                "  —  " + user.repositories().size()
                        + (user.repositories().size() == 1 ? " repository" : " repositories"),
                SMALL_MUTED));
        userHeader.setSpacingBefore(8);
        userHeader.setSpacingAfter(6);
        document.add(userHeader);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2.2f, 2.8f, 1.2f, 3.8f});
        table.setSpacingAfter(12);
        table.setHeaderRows(1);

        table.addCell(headerCell("Repository"));
        table.addCell(headerCell("Full name"));
        table.addCell(headerCell("Permission"));
        table.addCell(headerCell("Access details"));

        int row = 0;
        for (RepositoryAccessDto repo : user.repositories()) {
            Color bg = (row % 2 == 0) ? Color.WHITE : ROW_ALT;
            table.addCell(bodyCell(repo.name(), bg));
            table.addCell(bodyCell(repo.fullName(), bg));
            table.addCell(bodyCell(repo.permission() != null ? repo.permission().getValue() : "-", bg));
            table.addCell(bodyCell(buildAccessDetail(repo), bg));
            row++;
        }

        document.add(table);
    }

    private String buildAccessDetail(RepositoryAccessDto repo) {
        StringBuilder detail = new StringBuilder();
        if (repo.accessDescription() != null && !repo.accessDescription().isBlank()) {
            detail.append(repo.accessDescription());
        }
        AccessPermissionsDto permissions = repo.permissions();
        if (permissions != null) {
            if (!detail.isEmpty()) {
                detail.append("\n");
            }
            detail.append("Flags: ")
                    .append(flag("admin", permissions.admin())).append("  ")
                    .append(flag("maintain", permissions.maintain())).append("  ")
                    .append(flag("write", permissions.write())).append("  ")
                    .append(flag("triage", permissions.triage())).append("  ")
                    .append(flag("read", permissions.read()));
        }
        return detail.isEmpty() ? "-" : detail.toString();
    }

    private String flag(String name, boolean enabled) {
        return name + "=" + (enabled ? "yes" : "no");
    }

    private PdfPCell headerCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, TABLE_HEADER_FONT));
        cell.setBackgroundColor(ACCENT);
        cell.setPadding(7);
        cell.setBorderColor(ACCENT);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private PdfPCell bodyCell(String text, Color background) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "-" : text, TABLE_CELL_FONT));
        cell.setBackgroundColor(background);
        cell.setPadding(6);
        cell.setBorderColor(BORDER);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        return cell;
    }

    private int countMappings(AccessReportResponse report) {
        return report.users().stream()
                .mapToInt(user -> user.repositories().size())
                .sum();
    }

    private static final class PageNumberFooter extends PdfPageEventHelper {

        private final String organization;
        private final Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, TEXT_MUTED);

        private PageNumberFooter(String organization) {
            this.organization = organization;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfPTable footer = new PdfPTable(2);
            try {
                footer.setWidths(new int[]{70, 30});
                footer.setTotalWidth(document.right() - document.left());
                footer.getDefaultCell().setBorder(Rectangle.TOP);
                footer.getDefaultCell().setBorderColor(BORDER);
                footer.getDefaultCell().setPaddingTop(6);

                footer.getDefaultCell().setHorizontalAlignment(Element.ALIGN_LEFT);
                footer.addCell(new Phrase("GitHub Access Report — " + organization, footerFont));

                footer.getDefaultCell().setHorizontalAlignment(Element.ALIGN_RIGHT);
                footer.addCell(new Phrase("Page " + writer.getPageNumber(), footerFont));

                footer.writeSelectedRows(0, -1, document.left(), document.bottom() - 10, writer.getDirectContent());
            } catch (DocumentException ignored) {
                // Footer failure should not break PDF generation
            }
        }
    }
}
