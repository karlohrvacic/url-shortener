package cc.hrva.urlshortener.service.impl;

import cc.hrva.urlshortener.configuration.properties.AppProperties;
import cc.hrva.urlshortener.converter.CreateUrlToUrlConverter;
import cc.hrva.urlshortener.converter.UrlToPeekUrlConverter;
import cc.hrva.urlshortener.converter.UrlUpdateDtoToUrlConverter;
import cc.hrva.urlshortener.dto.CreateUrlDto;
import cc.hrva.urlshortener.dto.UrlResponse;
import cc.hrva.urlshortener.dto.UrlSearchDto;
import cc.hrva.urlshortener.dto.UrlUpdateDto;
import cc.hrva.urlshortener.exception.UrlNotFoundException;
import cc.hrva.urlshortener.model.ApiKey;
import cc.hrva.urlshortener.model.PeekUrl;
import cc.hrva.urlshortener.model.Url;
import cc.hrva.urlshortener.model.User;
import cc.hrva.urlshortener.repository.UrlRepository;
import cc.hrva.urlshortener.repository.specification.UrlSpecification;
import cc.hrva.urlshortener.service.ApiKeyService;
import cc.hrva.urlshortener.service.IPAddressService;
import cc.hrva.urlshortener.service.UrlService;
import cc.hrva.urlshortener.service.UserService;
import cc.hrva.urlshortener.validator.ApiKeyValidator;
import cc.hrva.urlshortener.validator.UrlValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.view.RedirectView;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DefaultUrlService implements UrlService {

    private final UserService userService;
    private final UrlValidator urlValidator;
    private final ApiKeyService apiKeyService;
    private final UrlRepository urlRepository;
    private final AppProperties appProperties;
    private final ApiKeyValidator apiKeyValidator;
    private final IPAddressService ipAddressService;
    private final TaskExecutor applicationTaskExecutor;
    private final UrlToPeekUrlConverter urlToPeekUrlConverter;
    private final CreateUrlToUrlConverter createUrlToUrlConverter;
    private final UrlUpdateDtoToUrlConverter urlUpdateDtoToUrlConverter;
    private final CacheManager cacheManager;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public UrlResponse saveUrlRouting(final @Valid CreateUrlDto createUrlDto) {
        final var url = createUrlToUrlConverter.convert(createUrlDto);

        log.info("Creating URL longUrl={} authenticated={}", createUrlDto.getLongUrl(), userService.getUserFromToken() != null);

        urlValidator.longUrlInUrl(url);
        urlValidator.checkIfUrlSafe(url);
        urlValidator.checkIfUrlExpirationDateIsInThePast(url);

        if (userService.getUserFromToken() != null) {
            return saveUrlWithApiKey(createUrlDto, null);
        }
        return UrlResponse.from(createUrlForAnonymousUser(url));
    }

    @Override
    @Transactional
    public UrlResponse saveUrlWithApiKey(final @Valid CreateUrlDto createUrlDto, final String key) {
        final var url = createUrlToUrlConverter.convert(createUrlDto);

        log.info("Creating URL with API key longUrl={}", createUrlDto.getLongUrl());

        final ApiKey apiKey = getApiKey(key);
        setShortUrlForLoggedInUser(url, apiKey);

        log.info("Saving URL");
        return UrlResponse.from(urlRepository.save(url));
    }

    @Override
    public Page<UrlResponse> getAllMyUrls(final String apiKey, final Pageable pageable,
            final UrlSearchDto search) {
        final User user;
        if (StringUtils.isNotEmpty(apiKey)) {
            apiKeyValidator.apiKeyExistsByKeyAndIsValid(apiKey);
            user = apiKeyService.fetchApiKeyByKey(apiKey).getOwner();
        } else {
            user = userService.getUserFromToken();
        }

        var spec = Specification.where(UrlSpecification.hasOwner(user));

        if (StringUtils.isNotEmpty(search.getSearch())) {
            spec = spec.and(UrlSpecification.search(search.getSearch()));
        }
        if (search.getActive() != null) {
            spec = spec.and(UrlSpecification.hasActive(search.getActive()));
        }
        if (search.getExpired() != null && search.getExpired()) {
            spec = spec.and(UrlSpecification.isExpired());
        }
        if (search.getDateFrom() != null) {
            spec = spec.and(UrlSpecification.createdAfter(search.getDateFrom()));
        }
        if (search.getDateTo() != null) {
            spec = spec.and(UrlSpecification.createdBefore(search.getDateTo()));
        }

        return urlRepository.findAll(spec, pageable).map(UrlResponse::from);
    }

    @Override
    public Page<UrlResponse> getAllUrls(final Pageable pageable, final UrlSearchDto search) {
        Specification<Url> spec = null;

        if (StringUtils.isNotEmpty(search.getSearch())) {
            spec = UrlSpecification.search(search.getSearch());
        }
        if (search.getActive() != null) {
            spec = (spec == null) ? UrlSpecification.hasActive(search.getActive()) : spec.and(UrlSpecification.hasActive(search.getActive()));
        }
        if (search.getExpired() != null && search.getExpired()) {
            spec = (spec == null) ? UrlSpecification.isExpired() : spec.and(UrlSpecification.isExpired());
        }
        if (search.getDateFrom() != null) {
            spec = (spec == null) ? UrlSpecification.createdAfter(search.getDateFrom()) : spec.and(UrlSpecification.createdAfter(search.getDateFrom()));
        }
        if (search.getDateTo() != null) {
            spec = (spec == null) ? UrlSpecification.createdBefore(search.getDateTo()) : spec.and(UrlSpecification.createdBefore(search.getDateTo()));
        }

        if (spec == null) {
            return urlRepository.findAll(pageable).map(UrlResponse::from);
        }
        return urlRepository.findAll(spec, pageable).map(UrlResponse::from);
    }

    @Override
    @Transactional
    @CacheEvict(value = "urls", key = "#result.shortUrl")
    public UrlResponse revokeUrl(final Long id) {
        log.info("Revoke URL id={}", id);

        final var url = urlRepository.findById(id).orElseThrow(() -> new UrlNotFoundException("Url doesn't exist"));

        urlValidator.verifyUserAdminOrOwner(url);

        return UrlResponse.from(urlRepository.save(deactivateUrl(url)));
    }

    @Override
    @Transactional
    @CacheEvict(value = "urls", key = "#result.shortUrl")
    public UrlResponse activateUrl(final Long id) {
        log.info("Activate URL id={}", id);

        final var url = urlRepository.findById(id).orElseThrow(() -> new UrlNotFoundException("Url doesn't exist"));

        urlValidator.verifyUserAdminOrOwner(url);

        return UrlResponse.from(urlRepository.save(activateUrl(url)));
    }

    @Override
    @Transactional
    @CacheEvict(value = "urls", allEntries = true)
    public void deleteUrl(final Long id) {
        log.info("Delete URL id={}", id);

        final var url = urlRepository.findById(id).orElseThrow(() -> new UrlNotFoundException("Url doesn't exist"));

        urlValidator.verifyUserAdminOrOwner(url);

        ipAddressService.deleteRecordsForUrl(url);
        urlRepository.delete(url);
    }

    @Override
    @Transactional
    @CacheEvict(value = "urls", key = "#result.shortUrl")
    public UrlResponse updateUrl(final UrlUpdateDto updateDto) {
        log.info("Update URL id={}", updateDto.getId());

        final var url = urlUpdateDtoToUrlConverter.convert(updateDto);
        urlValidator.checkIfUrlExpirationDateIsInThePast(url);

        url.verifyUrlValidity(url);
        return UrlResponse.from(urlRepository.save(url));
    }

    @Override
    @Transactional
    public UrlResponse checkIPUniquenessAndReturnUrl(final String shortUrl, final String clientIP) {
        log.info("URL access shortUrl={} clientIP={}", shortUrl, clientIP);

        final var url = findUrlByShortUrlAndActive(shortUrl);

        asyncCheckIfVisitUnique(clientIP, url);
        return UrlResponse.from(url);
    }

    @Override
    public byte[] exportMyUrlsAsCsv(final String apiKey) {
        final var user = getUserForExport(apiKey);
        final var urls = user != null
                ? urlRepository.findAllByOwner(user, Pageable.unpaged()).getContent()
                : List.<Url>of();

        final var sb = new StringBuilder();
        sb.append("Short URL,Long URL,Visits,Visit Limit,Created,Expires,Active\n");
        for (final var url : urls) {
            sb.append(String.format("%s,%s,%d,%d,%s,%s,%b\n",
                    url.getShortUrl(),
                    escapeCsv(url.getLongUrl()),
                    url.getVisits(),
                    url.getVisitLimit() != null ? url.getVisitLimit() : 0,
                    url.getCreateDate() != null ? url.getCreateDate().toString() : "",
                    url.getExpirationDate() != null ? url.getExpirationDate().toString() : "",
                    url.isActive()));
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCsv(final String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private User getUserForExport(final String apiKey) {
        if (StringUtils.isNotEmpty(apiKey)) {
            apiKeyValidator.apiKeyExistsByKeyAndIsValid(apiKey);
            return apiKeyService.fetchApiKeyByKey(apiKey).getOwner();
        }
        return userService.getUserFromToken();
    }

    @Override
    public PeekUrl peekUrlByShortUrl(final String shortUrl) {
        final var cache = cacheManager.getCache("urls");
        if (cache != null) {
            final var cached = Optional.ofNullable(cache.get(shortUrl))
                    .map(org.springframework.cache.Cache.ValueWrapper::get)
                    .map(v -> v instanceof PeekUrl pk ? pk : null)
                    .orElse(null);
            if (cached != null) {
                return cached;
            }
        }

        final var url = findUrlByShortUrlAndActive(shortUrl);
        final var peekUrl = urlToPeekUrlConverter.convert(url);

        if (cache != null) {
            cache.put(shortUrl, peekUrl);
        }

        return peekUrl;
    }

    @Override
    @Transactional
    public void deactivateExpiredUrls() {
        final var urls = urlRepository.findByExpirationDateLessThanEqualAndActiveTrue(LocalDateTime.now()).stream()
                .map(this::deactivateUrl)
                .toList();

        urlRepository.saveAll(urls);
        if (!urls.isEmpty()) log.info("Deactivated {} urls", urls.size());
    }

    @Override
    public RedirectView redirectResultUrl(final String shortUrl, final String clientIP) {
        log.info("Redirect shortUrl={} clientIP={}", shortUrl, clientIP);

        final var redirectView = new RedirectView();

        if (StringUtils.isNotEmpty(shortUrl)) {
            // Try cache first
            final var cache = cacheManager.getCache("urls");
            if (cache != null) {
                final var cached = Optional.ofNullable(cache.get(shortUrl))
                        .map(org.springframework.cache.Cache.ValueWrapper::get)
                        .map(v -> v instanceof UrlResponse ur ? ur : mapToUrlResponse(v))
                        .orElse(null);
                if (cached != null) {
                    if (isUrlRedirectValid(cached)) {
                        redirectView.setUrl(cached.longUrl());
                        fireVisitTracking(shortUrl, clientIP);
                        return redirectView;
                    }
                    // Stale entry — evict and fall through
                    cache.evict(shortUrl);
                }
            }

            // Cache miss or invalid — fetch from DB
            final var urlOpt = urlRepository.findByShortUrlAndActiveTrue(shortUrl);
            if (urlOpt.isPresent()) {
                final var url = urlOpt.get();
                final var response = UrlResponse.from(url);
                if (cache != null) {
                    cache.put(shortUrl, response);
                }
                redirectView.setUrl(response.longUrl());
                asyncCheckIfVisitUnique(clientIP, url);
            } else {
                redirectView.setUrl(appProperties.getFrontendUrl());
            }
        } else {
            redirectView.setUrl(appProperties.getFrontendUrl());
        }

        return redirectView;
    }

    private boolean isUrlRedirectValid(final UrlResponse response) {
        if (!response.active()) return false;
        if (response.expirationDate() != null && LocalDateTime.now().isAfter(response.expirationDate())) return false;
        if (response.visitLimit() != null && response.visitLimit() > 0
                && response.visits() != null && response.visits() >= response.visitLimit()) return false;
        return true;
    }

    @SuppressWarnings("unchecked")
    private UrlResponse mapToUrlResponse(final Object value) {
        if (value instanceof UrlResponse ur) return ur;
        if (value instanceof LinkedHashMap<?, ?> map) {
            try {
                return objectMapper.convertValue(map, UrlResponse.class);
            } catch (final Exception e) {
                log.warn("Failed to convert cached value to UrlResponse", e);
                return null;
            }
        }
        return null;
    }

    private void fireVisitTracking(final String shortUrl, final String clientIP) {
        applicationTaskExecutor.execute(() -> {
            urlRepository.findByShortUrlAndActiveTrue(shortUrl).ifPresent(url -> {
                if (!ipAddressService.urlAlreadyVisitedByIP(url, clientIP)) {
                    urlRepository.save(url.onVisit());
                }
            });
        });
    }

    @Override
    public String generateShortUrl(final Long length) {
        return RandomStringUtils.random(Math.toIntExact(length), true, true);
    }

    private Url findUrlByShortUrlAndActive(final String shortUrl) {
        return urlRepository.findByShortUrlAndActiveTrue(shortUrl).orElseThrow(() -> new UrlNotFoundException("URL doesn't exist"));
    }

    @Override
    public Url getUrlByLongUrl(final String longUrl) {
        return urlRepository.findByLongUrlAndActiveTrue(longUrl).orElseThrow(() -> new UrlNotFoundException("URL doesn't exist"));
    }

    private Url createUrlForAnonymousUser(final Url url) {
        urlValidator.checkIfAnonymousUrlCreationEnabled();
        url.clearForAnonymousUser();

        if (urlRepository.existsUrlByLongUrlAndActiveTrueAndOwnerIsNull(url.getLongUrl())) {
            final var existingLongUrl = getUrlByLongUrl(url.getLongUrl());
            log.warn("Long url already exists in DB, will return URL from long URL {}", existingLongUrl.getShortUrl());
            return existingLongUrl;
        }

        url.setShortUrl(generateShortUrl(appProperties.getShortUrlLength()));

        urlValidator.checkIfShortUrlIsUnique(url.getShortUrl());
        urlValidator.checkIfShortUrlIsReserved(url.getShortUrl());

        log.info("Saving URL");
        return urlRepository.save(url);
    }

    private ApiKey getApiKey(String key) {
        final ApiKey apiKey;

        if (StringUtils.isNotEmpty(key)) {
            apiKey = apiKeyService.fetchApiKeyByKey(key);
        } else {
            apiKey = getFirstApiKeyForLoggedInUser();
            key = apiKey.getKey();
        }

        apiKeyValidator.apiKeyExistsByKeyAndIsValid(key);

        return apiKey;
    }

    private void asyncCheckIfVisitUnique(final String clientIP, final Url url) {
        applicationTaskExecutor.execute(() -> {
            if (!ipAddressService.urlAlreadyVisitedByIP(url, clientIP)) {
                incrementVisitForUrl(url);
            }
        });
    }

    private void incrementVisitForUrl(final Url url) {
        urlRepository.save(url.onVisit());
    }

    private void setShortUrlForLoggedInUser(final @Valid Url url, final ApiKey apiKey) {
        log.info("Setting short URL");

        if (StringUtils.isEmpty(url.getShortUrl())) {
            url.setShortUrl(generateShortUrl(appProperties.getShortUrlLength()));
            log.info("URL got generated short URL {}", url.getShortUrl());
        }

        urlValidator.checkIfShortUrlIsUnique(url.getShortUrl());
        urlValidator.checkIfShortUrlIsReserved(url.getShortUrl());

        url.setApiKey(apiKey);
        url.setOwner(apiKey.getOwner());
        apiKeyService.apiKeyUseAction(apiKey);

        log.info("URL is created with API key and is keeping custom short url: {}", url.getShortUrl());
    }

    private ApiKey getFirstApiKeyForLoggedInUser() {
        log.info("User is authenticated but didn't pass API key");

        final var user = userService.getUserFromToken();
        return user.getApiKeys()
                .stream()
                .filter(ApiKey::isActive)
                .findFirst()
                .orElseGet(() -> apiKeyService.findApiKeyByKey(apiKeyService.generateNewApiKey().key()));
    }

    public Url deactivateUrl(Url url) {
        url.setActive(false);
        return url;
    }

    public Url activateUrl(Url url) {
        url.setActive(true);
        return url;
    }

}
