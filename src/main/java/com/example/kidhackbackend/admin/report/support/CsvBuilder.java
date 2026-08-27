package com.example.kidhackbackend.admin.report.support;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * RFC 4180 형식의 CSV 본문을 만든다.
 *
 * <p>엑셀에서 더블클릭으로 열었을 때 한글이 깨지지 않도록 UTF-8 BOM을 앞에 붙인다.
 */
public class CsvBuilder {

    private static final String BOM = "\uFEFF";
    private static final String LINE_SEPARATOR = "\r\n";
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StringBuilder body = new StringBuilder(BOM);

    public CsvBuilder(String... headers) {
        row((Object[]) headers);
    }

    public CsvBuilder row(Object... values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) body.append(',');
            body.append(escape(values[i]));
        }
        body.append(LINE_SEPARATOR);
        return this;
    }

    public byte[] toBytes() {
        return body.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** 날짜/시간 컬럼 표기를 통일한다. null이면 빈 칸. */
    public static String format(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(TIMESTAMP) : "";
    }

    private static String escape(Object value) {
        if (value == null) return "";
        String text = String.valueOf(value);
        if (text.indexOf('"') >= 0 || text.indexOf(',') >= 0
                || text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0) {
            return '"' + text.replace("\"", "\"\"") + '"';
        }
        return text;
    }
}
