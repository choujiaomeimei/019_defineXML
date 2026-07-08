package com.stat.service.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Unified Excel styling: 宋体 for Chinese, Times New Roman for English,
 * colored header row, thin borders, consistent formatting.
 */
public final class ExcelStyleHelper {

    private static final String FONT_CN = "\u5B8B\u4F53";
    private static final String FONT_EN = "Times New Roman";
    private static final short FONT_SIZE_HEADER = 11;
    private static final short FONT_SIZE_BODY = 10;
    private static final byte[] HEADER_BG_RGB = {(byte) 0x43, (byte) 0x38, (byte) 0xCA};

    private ExcelStyleHelper() {}

    public static CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();

        Font font = wb.createFont();
        font.setFontName(FONT_EN);
        font.setBold(true);
        font.setFontHeightInPoints(FONT_SIZE_HEADER);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);

        if (style instanceof XSSFCellStyle xStyle) {
            xStyle.setFillForegroundColor(new XSSFColor(HEADER_BG_RGB, null));
        } else {
            style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        }
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        applyBorders(style, BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);

        return style;
    }

    public static CellStyle createBodyStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();

        Font font = wb.createFont();
        font.setFontName(FONT_EN);
        font.setFontHeightInPoints(FONT_SIZE_BODY);
        style.setFont(font);

        applyBorders(style, BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(false);

        return style;
    }

    /**
     * Apply header + body styles to all rows in a sheet, auto-size columns,
     * and set a fixed header row height.
     */
    public static void applyStylesToSheet(Sheet sheet, CellStyle headerStyle, CellStyle bodyStyle) {
        int lastRow = sheet.getLastRowNum();
        int maxCol = 0;

        for (int r = 0; r <= lastRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            CellStyle target = (r == 0) ? headerStyle : bodyStyle;
            for (int c = 0; c < row.getLastCellNum(); c++) {
                Cell cell = row.getCell(c);
                if (cell == null) cell = row.createCell(c);
                cell.setCellStyle(target);
            }
            if (row.getLastCellNum() > maxCol) maxCol = row.getLastCellNum();
        }

        Row headerRow = sheet.getRow(0);
        if (headerRow != null) {
            headerRow.setHeightInPoints(24);
        }

        for (int c = 0; c < maxCol; c++) {
            sheet.autoSizeColumn(c);
            int w = sheet.getColumnWidth(c);
            sheet.setColumnWidth(c, Math.min(w + 512, 15000));
        }
    }

    /**
     * Convenience: style every sheet in the workbook.
     */
    public static void styleWorkbook(Workbook wb) {
        CellStyle headerStyle = createHeaderStyle(wb);
        CellStyle bodyStyle = createBodyStyle(wb);
        for (int i = 0; i < wb.getNumberOfSheets(); i++) {
            applyStylesToSheet(wb.getSheet(wb.getSheetName(i)), headerStyle, bodyStyle);
        }
    }

    private static void applyBorders(CellStyle style, BorderStyle borderStyle) {
        style.setBorderTop(borderStyle);
        style.setBorderBottom(borderStyle);
        style.setBorderLeft(borderStyle);
        style.setBorderRight(borderStyle);
        style.setTopBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setBottomBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setLeftBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setRightBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
    }
}
