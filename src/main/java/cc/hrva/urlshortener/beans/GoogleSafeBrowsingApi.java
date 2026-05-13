package cc.hrva.urlshortener.beans;

import cc.hrva.urlshortener.configuration.properties.GoogleApiProperties;
import com.google.api.services.safebrowsing.v5.Safebrowsing;
import com.google.api.services.safebrowsing.v5.model.GoogleSecuritySafebrowsingV5SearchUrlsResponse;
import com.google.api.services.safebrowsing.v5.model.GoogleSecuritySafebrowsingV5ThreatUrl;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GoogleSafeBrowsingApi {

    private final GoogleApiProperties googleApiProperties;

    public Safebrowsing.Urls.Search createUrlSearchRequest(final Safebrowsing safebrowsing, final List<String> urls) {
        try {
            return safebrowsing.urls()
                    .search()
                    .setUrls(urls);
        } catch (final IOException exception) {
            throw new RuntimeException("Failed to create Safe Browsing search request", exception);
        }
    }

    public GoogleSecuritySafebrowsingV5SearchUrlsResponse executeSearch(final Safebrowsing.Urls.Search search) throws IOException {
        return search
                .setKey(googleApiProperties.getKey())
                .execute();
    }

    public List<String> extractThreatUrls(final GoogleSecuritySafebrowsingV5SearchUrlsResponse response) {
        if (response == null || response.getThreats() == null) {
            return List.of();
        }

        return response.getThreats().stream()
                .map(GoogleSecuritySafebrowsingV5ThreatUrl::getUrl)
                .toList();
    }

}
