package cc.hrva.urlshortener.service.impl;

import cc.hrva.urlshortener.beans.GoogleSafeBrowsingApi;
import cc.hrva.urlshortener.configuration.properties.GoogleApiProperties;
import cc.hrva.urlshortener.service.SafeBrowsingService;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.safebrowsing.v5.Safebrowsing;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Service;

@Service
@CommonsLog
@RequiredArgsConstructor
public class DefaultSafeBrowsingService implements SafeBrowsingService {

    private final GoogleApiProperties googleApiProperties;
    private final GoogleSafeBrowsingApi googleSafeBrowsingApi;

    @Override
    public List<String> checkUrlsForThreats(final List<String> urls) {
        try {
            final var httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            final var jsonFactory = GsonFactory.getDefaultInstance();

            final var safebrowsing = new Safebrowsing.Builder(httpTransport, jsonFactory, null)
                    .setApplicationName(googleApiProperties.getClientId())
                    .build();

            final var searchRequest = googleSafeBrowsingApi.createUrlSearchRequest(safebrowsing, urls);
            final var response = googleSafeBrowsingApi.executeSearch(searchRequest);

            return googleSafeBrowsingApi.extractThreatUrls(response);
        } catch (final IOException exception) {
            log.error("Safe Browsing error", exception);
        } catch (final GeneralSecurityException exception) {
            log.error("Safe Browsing error", exception);
            throw new RuntimeException(exception);
        }

        return new ArrayList<>();
    }

    @Override
    public List<String> checkUrlsForThreats(final String url) {
        return checkUrlsForThreats(List.of(url));
    }

}
