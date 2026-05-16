package cc.hrva.urlshortener.service.impl;

import cc.hrva.urlshortener.client.GoogleSafeBrowsingApi;
import cc.hrva.urlshortener.service.SafeBrowsingService;
import com.google.api.services.safebrowsing.v5.Safebrowsing;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultSafeBrowsingService implements SafeBrowsingService {

    private final Safebrowsing safebrowsing;
    private final GoogleSafeBrowsingApi googleSafeBrowsingApi;

    @Override
    public List<String> checkUrlsForThreats(final List<String> urls) {
        log.debug("SafeBrowsing check urls={}", urls);
        try {
            final var searchRequest = googleSafeBrowsingApi.createUrlSearchRequest(safebrowsing, urls);
            final var response = googleSafeBrowsingApi.executeSearch(searchRequest);
            return googleSafeBrowsingApi.extractThreatUrls(response);
        } catch (final IOException exception) {
            log.error("Safe Browsing API error", exception);
            return List.of();
        }
    }

    @Override
    public List<String> checkUrlsForThreats(final String url) {
        return checkUrlsForThreats(List.of(url));
    }

}
