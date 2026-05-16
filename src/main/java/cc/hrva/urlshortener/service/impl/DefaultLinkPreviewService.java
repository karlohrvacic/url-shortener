package cc.hrva.urlshortener.service.impl;

import cc.hrva.urlshortener.configuration.properties.AppProperties;
import cc.hrva.urlshortener.dto.LinkPreviewResponse;
import cc.hrva.urlshortener.service.LinkPreviewService;
import java.time.Duration;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DefaultLinkPreviewService implements LinkPreviewService {

    private static final Logger log = LoggerFactory.getLogger(DefaultLinkPreviewService.class);

    @Override
    public LinkPreviewResponse fetchPreview(final String url) {
        try {
            final var doc = Jsoup.connect(url)
                    .timeout((int) Duration.ofSeconds(5).toMillis())
                    .followRedirects(true)
                    .userAgent("Mozilla/5.0 (compatible; UrlShortenerBot/1.0)")
                    .get();

            return new LinkPreviewResponse(
                    url,
                    extractMeta(doc, "og:title", "twitter:title"),
                    extractMeta(doc, "og:description", "description", "twitter:description"),
                    extractMeta(doc, "og:image", "twitter:image")
            );
        } catch (final Exception exception) {
            log.warn("Failed to fetch preview for url={}", url, exception);
            return new LinkPreviewResponse(url, null, null, null);
        }
    }

    private String extractMeta(final Document doc, final String... metaNames) {
        for (final var name : metaNames) {
            final var tag = doc.selectFirst("meta[name=" + name + "], meta[property=" + name + "]");
            if (tag != null) {
                final var content = tag.attr("content");
                if (!content.isBlank()) {
                    return content;
                }
            }
        }

        final var title = doc.title();
        return title.isBlank() ? null : title;
    }

}
