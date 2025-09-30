package io.github.repoboard.common.util;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class SanitizeUtil {

    /**
     * 검색어(q) 전용 정제 메서드 <br>
     * - 클라이언트가 encodeURIComponent로 보낼 수 있으므로 디코드 후 처리<br>
     * - Markdown 링크/이미지, on* 속성, javascript:, data: 스킴 제거<br>
     * - 위험 문자 제거 및 길이 제한 적용
     */
    public static String sanitizeQuery(String raw) {
        if (raw == null || raw.isBlank()) return "";

        // 0) 클라이언트가 encodeURIComponent 했을 가능성 고려 -> 디코드
        String decoded = URLDecoder.decode(raw, StandardCharsets.UTF_8);

        // 1) HTML 태그 제거
        String s = Jsoup.clean(decoded, Safelist.none());

        // 2) Markdown 이미지/링크 제거: ![alt](url) / [text](url)
        s = s.replaceAll("!\\[[^\\]]*\\]\\([^)]*\\)", "");
        s = s.replaceAll("\\[[^\\]]*\\]\\([^)]*\\)", "");

        // 3) attribute-like injection 제거 (onerror, onclick 등)
        s = s.replaceAll("(?i)\\s+on\\w+\\s*=\\s*\"[^\"]*\"", "");
        s = s.replaceAll("(?i)\\s+on\\w+\\s*=\\s*'[^']*'", "");
        s = s.replaceAll("(?i)\\s+on\\w+\\s*=\\s*[^\\s>]+", "");

        // 4) javascript:, data: scheme 제거 (case-insensitive)
        s = s.replaceAll("(?i)javascript:\\s*[^\\s]*", "");
        s = s.replaceAll("(?i)data:[^\\s]*", "");

        // 5) 남은 위험 문자 제거 (Tomcat 400 방지)
        s = s.replaceAll("[<>\"'{}\\[\\]]", "");

        // 6) 공백 정리, 길이 제한 (예: 200자)
        s = s.replaceAll("\\s+", " ").trim();
        int maxLen = 200;
        if (s.length() > maxLen) s = s.substring(0, maxLen);

        return s;
    }
}