package io.github.repoboard.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * sitemap.xml을 동적으로 생성하는 컨트롤러.
 *
 * <p>검색 엔진 색인 대상은 공개 페이지만 포함한다.
 * 로그인/회원가입/프로필/설정 등 인증이 필요한 페이지는
 * 색인 대상이 아니므로 제외한다(GSC 색인 오류 방지).</p>
 *
 * <p>현재는 홈(/)만 노출한다. 검색 결과 페이지는 쿼리 파라미터 기반의
 * 동적 페이지라 고정 URL이 없어 sitemap에 포함하지 않는다.</p>
 */
@RestController
public class SitemapController {

    private static final String BASE_URL = "https://repoboard.kr";

    /**
     * sitemap.xml을 생성하여 반환한다.
     *
     * @return XML 형식의 sitemap 문자열
     */
    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemap() {
        String today = LocalDate.now().toString();

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        sb.append(url(BASE_URL + "/", today, "daily", "1.0"));
        sb.append("</urlset>");

        return sb.toString();
    }

    /**
     * 단일 <url> 항목을 생성한다.
     *
     * @param loc        페이지 URL
     * @param lastmod    최종 수정일 (yyyy-MM-dd)
     * @param changefreq 변경 빈도
     * @param priority   우선순위
     * @return XML <url> 블록 문자열
     */
    private String url(String loc, String lastmod, String changefreq, String priority) {
        return "  <url>\n"
                + "    <loc>" + escapeXml(loc) + "</loc>\n"
                + "    <lastmod>" + lastmod + "</lastmod>\n"
                + "    <changefreq>" + changefreq + "</changefreq>\n"
                + "    <priority>" + priority + "</priority>\n"
                + "  </url>\n";
    }

    /**
     * XML 특수문자를 이스케이프한다.
     *
     * @param raw 원본 문자열
     * @return 이스케이프된 문자열
     */
    private String escapeXml(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}