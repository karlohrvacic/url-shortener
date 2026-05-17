package cc.hrva.urlshortener.service.impl;

import cc.hrva.urlshortener.model.Url;
import cc.hrva.urlshortener.repository.UrlRepository;
import cc.hrva.urlshortener.repository.specification.UrlSpecification;
import cc.hrva.urlshortener.service.SafeBrowsingRecheckService;
import cc.hrva.urlshortener.service.SafeBrowsingService;
import cc.hrva.urlshortener.service.SendingEmailService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultSafeBrowsingRecheckService implements SafeBrowsingRecheckService {

    private static final int BATCH_SIZE = 20;

    private final UrlRepository urlRepository;
    private final SafeBrowsingService safeBrowsingService;
    private final SendingEmailService sendingEmailService;
    private final CacheManager cacheManager;

    @Override
    @Transactional
    public void recheckActiveUrls() {
        log.info("Starting SafeBrowsing recheck of active URLs");

        final var spec = Specification.where(UrlSpecification.hasActive(true));
        final var totalPages = urlRepository.findAll(spec, PageRequest.of(0, BATCH_SIZE)).getTotalPages();
        var checked = 0;
        var flagged = 0;

        for (int page = 0; page < totalPages; page++) {
            final var urls = urlRepository.findAll(spec, PageRequest.of(page, BATCH_SIZE));

            for (final var url : urls.getContent()) {
                final var threats = safeBrowsingService.checkUrlsForThreats(url.getLongUrl());

                if (!threats.isEmpty()) {
                    flagged++;
                    handleFlaggedUrl(url, threats);
                }

                url.setLastSafeBrowsingCheck(LocalDateTime.now());
                checked++;
            }

            urlRepository.saveAll(urls.getContent());
            log.debug("SafeBrowsing recheck progress: {}/{} checked, {} flagged", checked, totalPages * BATCH_SIZE, flagged);
        }

        log.info("SafeBrowsing recheck complete — {} checked, {} flagged and deactivated", checked, flagged);
    }

    private void handleFlaggedUrl(final Url url, final List<String> threats) {
        log.warn("URL /{} flagged as malicious: {}", url.getShortUrl(), threats);

        url.setActive(false);
        evictFromCache(url.getShortUrl());

        if (url.getOwner() != null) {
            sendingEmailService.sendEmailUrlMalwareDetected(url.getOwner(), url, String.join(", ", threats));
        }
    }

    private void evictFromCache(final String shortUrl) {
        try {
            final var cache = cacheManager.getCache("urls");
            if (cache != null) {
                cache.evict(shortUrl);
            }
        } catch (final Exception e) {
            log.debug("Failed to evict from cache", e);
        }
    }

}
