package io.github.repoboard.service;

import org.owasp.html.HtmlPolicyBuilder;
import org.springframework.stereotype.Service;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.owasp.html.PolicyFactory;

/**
 * {@code MarkdownService}는 Markdown 텍스트를 HTML로 변환하고, <br>
 * 변환된 HTML에 대해 보안 필터링(sanitization)을 적용하여 <br>
 * XSS(교차 사이트 스크립팅) 공격을 방지하는 역할을 수행하는 서비스입니다.
 *
 * <p>flexmark-java를 이용하여 Markdown을 HTML로 변환하고, <br>
 * OWASP Java HTML Sanitizer를 이용해 HTML 태그 및 속성 기반으로 필터링합니다.</p>
 *
 * <p>이 서비스는 GitHub README 렌더링 기반의 Markdown 출력합니다.</p>
 */
@Service
public class MarkdownService {

    private final Parser parser;
    private final HtmlRenderer renderer;
    private final PolicyFactory sanitizer;

    /**
     * Markdown 변환 및 HTML 보안 필터링을 위한 parser, renderer, sanitizer를 초기화합니다.
     *
     * <p>허용된 HTML 요소 및 속성은 OWASP 정책 기반으로 제한됩니다. <br>
     * 예를 들어 <code>&lt;script&gt;</code>, <code>onerror</code> 등의 위험한 태그/속성은 자동 제거됩니다.</p>
     */
    public MarkdownService(){
        MutableDataSet options = new MutableDataSet();
        this.parser = Parser.builder(options).build();
        this.renderer = HtmlRenderer.builder(options).build();
        this.sanitizer = new HtmlPolicyBuilder()
                .allowElements(
                        "a", "img", "p", "ul", "ol", "li", "code", "pre", "blockquote", "strong", "em", "h1", "h2", "h3", "h4", "h5", "h6",
                        "div", "span", "br", "table", "thead", "tbody", "tr", "th", "td"
                )
                .allowAttributes("href", "title", "target", "rel").onElements("a")
                .allowAttributes("src", "alt", "width", "height").onElements("img")
                .allowAttributes("align").onElements("div", "p", "img", "table", "th", "td")
                .allowAttributes("class").onElements("code", "div", "span", "table", "pre")
                .allowUrlProtocols("https", "http")
                .toFactory();
    }

    /**
     * Markdown 텍스트를 HTML로 변환한 후, 보안 필터링을 적용하여 안전한 HTML 문자열로 반환합니다.
     *
     * @param markdown 변환할 Markdown 문자열
     * @return 필터링된 안전한 HTML 문자열
     */
    public String toSafeHtml(String markdown){
        Node document = parser.parse(markdown);
        String rawHtml =  renderer.render(document);
        return sanitizer.sanitize(rawHtml);
    }
}