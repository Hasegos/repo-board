package io.github.repoboard.common.util;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

/**
 * 사용자 입력 문자열을 정제(sanitize)하는 유틸리티.
 *
 * <p>HTML, Markdown, JavaScript, XSS 관련 위험 요소를 제거한다.</p>
 */
public class SanitizeUtil {

    /**
     * 검색어(q) 전용 정제 메서드 <br>
     * - 클라이언트가 encodeURIComponent로 보낼 수 있으므로 디코드 후 처리<br>
     * - Markdown 링크/이미지, on* 속성, javascript:, data: 스킴 제거<br>
     * - 위험 문자 제거 및 길이 제한 적용
     */
    public static String sanitizeQuery(String raw) {
        if (raw == null || raw.isBlank()) return "";

        String s = Jsoup.clean(raw, Safelist.none());
        s = s.replaceAll("!\\[[^\\]]*\\]\\([^)]*\\)", "");
        s = s.replaceAll("\\[[^\\]]*\\]\\([^)]*\\)", "");
        s = s.replaceAll("(?i)\\s+on\\w+\\s*=\\s*\"[^\"]*\"", "");
        s = s.replaceAll("(?i)\\s+on\\w+\\s*=\\s*'[^']*'", "");
        s = s.replaceAll("(?i)\\s+on\\w+\\s*=\\s*[^\\s>]+", "");
        s = s.replaceAll("(?i)javascript:\\s*[^\\s]*", "");
        s = s.replaceAll("(?i)data:[^\\s]*", "");
        s = s.replaceAll("[<>\"'{}\\[\\]]", "");
        s = s.replaceAll("\\s+", " ").trim();

        if (s.length() > 200) s = s.substring(0, 200);

        return s;
    }
}