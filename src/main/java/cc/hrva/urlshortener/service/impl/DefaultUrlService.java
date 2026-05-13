package cc.hrva.urlshortener.service.impl;

import cc.hrva.urlshortener.configuration.properties.AppProperties;
import cc.hrva.urlshortener.converter.CreateUrlToUrlConverter;
import cc.hrva.urlshortener.converter.UrlToPeekUrlConverter;
import cc.hrva.urlshortener.converter.UrlUpdateDtoToUrlConverter;
import cc.hrva.urlshortener.dto.CreateUrlDto;
import cc.hrva.urlshortener.dto.UrlUpdateDto;
import cc.hrva.urlshortener.exception.UrlNotFoundException;
import cc.hrva.urlshortener.model.ApiKey;
import cc.hrva.urlshortener.model.PeekUrl;
import cc.hrva.urlshortener.model.Url;
import cc.hrva.urlshortener.model.User;
import cc.hrva.urlshortener.repository.UrlRepository;
import cc.hrva.urlshortener.service.ApiKeyService;
import cc.hrva.urlshortener.service.IPAddressService;
import cc.hrva.urlshortener.service.UrlService;
import cc.hrva.urlshortener.service.UserService;
import cc.hrva.urlshortener.validator.ApiKeyValidator;
import cc.hrva.urlshortener.validator.UrlValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.view.RedirectView;

import java.time.LocalDateTime;
import java.util.List;

@Service
@CommonsLog
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

    @Override
    @Transactional
    public RedirectView redirectResultUrl(final String shortUrl, final String clientIP) {
        final RedirectView redirectView = new RedirectView();

        if (StringUtils.isNotEmpty(shortUrl) && urlRepository.existsUrlByShortUrlAndActiveTrue(shortUrl)) {
            redirectView.setUrl(checkIPUniquenessAndReturnUrl(shortUrl, clientIP).getLongUrl());
        } else {
            redirectView.setUrl(appProperties.getFrontendUrl());
        }

        return redirectView;
    }

    @Override
    @Transactional
    public Url saveUrlRouting(final @Valid CreateUrlDto createUrlDto) {
        final var url = createUrlToUrlConverter.convert(createUrlDto);

        urlValidator.longUrlInUrl(url);
        urlValidator.checkIfUrlSafe(url);
        urlValidator.checkIfUrlExpirationDateIsInThePast(url);

        if (userService.getUserFromToken() != null) {
            return saveUrlWithApiKey(createUrlDto, null);
        }
        return createUrlForAnonymousUser(url);
    }

    @Override
    @Transactional
    public Url saveUrlWithApiKey(final @Valid CreateUrlDto createUrlDto, final String key) {
        final var url = createUrlToUrlConverter.convert(createUrlDto);
        final ApiKey apiKey = getApiKey(key);
        setShortUrlForLoggedInUser(url, apiKey);

        log.info("Saving URL");
        return urlRepository.save(url);
    }

    @Override
    public List<Url> getAllMyUrls(final String apiKey) {
        final User user;
        if (StringUtils.isNotEmpty(apiKey)) {
            apiKeyValidator.apiKeyExistsByKeyAndIsValid(apiKey);
            user = apiKeyService.fetchApiKeyByKey(apiKey).getOwner();
        } else {
            user = userService.getUserFromToken();
        }

        return urlRepository.findAllByOwner(user).orElse(null);
    }

    @Override
    public List<Url> getAllUrls() {
        return urlRepository.findAll();
    }

    @Override
    public Url revokeUrl(final Long id) {
        final var url = urlRepository.findById(id).orElseThrow(() -> new UrlNotFoundException("Url doesn't exist"));

        urlValidator.verifyUserAdminOrOwner(url);

        return urlRepository.save(deactivateUrl(url));
    }

    @Override
    @Transactional
    public void deleteUrl(final Long id) {
        final var url = urlRepository.findById(id).orElseThrow(() -> new UrlNotFoundException("Url doesn't exist"));

        urlValidator.verifyUserAdminOrOwner(url);

        ipAddressService.deleteRecordsForUrl(url);
        urlRepository.delete(url);
    }

    @Override
    @Transactional
    public Url updateUrl(final UrlUpdateDto updateDto) {
        final var url = urlUpdateDtoToUrlConverter.convert(updateDto);
        urlValidator.checkIfUrlExpirationDateIsInThePast(url);

        url.verifyUrlValidity(url);
        return urlRepository.save(url);
    }

    @Override
    @Transactional
    public Url checkIPUniquenessAndReturnUrl(final String shortUrl, final String clientIP) {
        final var url = findUrlByShortUrlAndActive(shortUrl);

        asyncCheckIfVisitUnique(clientIP, url);
        return url;
    }

    @Override
    public PeekUrl peekUrlByShortUrl(final String shortUrl) {
        final var url = findUrlByShortUrlAndActive(shortUrl);

        return urlToPeekUrlConverter.convert(url);
    }

    @Override
    public void deactivateExpiredUrls() {
        final var urls = urlRepository.findByExpirationDateLessThanEqualAndActiveTrue(LocalDateTime.now()).stream()
                .map(this::deactivateUrl)
                .toList();

        urlRepository.saveAll(urls);
        if (!urls.isEmpty()) log.info(String.format("Deactivated %d urls", urls.size()));
    }

    @Override
    public Url getUrlByLongUrl(final String longUrl) {
        return urlRepository.findByLongUrlAndActiveTrue(longUrl).orElseThrow(() -> new UrlNotFoundException("URL doesn't exist"));
    }

    @Override
    public String generateShortUrl(final Long length) {
        return RandomStringUtils.random(Math.toIntExact(length), true, true);
    }

    private Url findUrlByShortUrlAndActive(final String shortUrl) {
        return urlRepository.findByShortUrlAndActiveTrue(shortUrl).orElseThrow(() -> new UrlNotFoundException("URL doesn't exist"));
    }

    private Url createUrlForAnonymousUser(final Url url) {
        urlValidator.checkIfAnonymousUrlCreationEnabled();
        url.clearForAnonymousUser();

        if (urlRepository.existsUrlByLongUrlAndActiveTrueAndOwnerIsNull(url.getLongUrl())) {
            final var existingLongUrl = getUrlByLongUrl(url.getLongUrl());
            log.warn("Long url already exists in DB, will return URL from long URL " + existingLongUrl.getShortUrl());
            return existingLongUrl;
        }

        url.setShortUrl(generateShortUrl(appProperties.getShortUrlLength()));

        urlValidator.checkIfShortUrlIsUnique(url.getShortUrl());

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
            log.info(String.format("URL got generated short URL %s", url.getShortUrl()));
        }

        urlValidator.checkIfShortUrlIsUnique(url.getShortUrl());

        url.setApiKey(apiKey);
        url.setOwner(apiKey.getOwner());
        apiKeyService.apiKeyUseAction(apiKey);

        log.info("URL is created with API key and is keeping custom short url: " + url.getShortUrl());
    }

    private ApiKey getFirstApiKeyForLoggedInUser() {
        log.info("User is authenticated but didn't pass API key");

        return userService.getUserFromToken()
                .getApiKeys()
                .stream()
                .filter(ApiKey::isActive)
                .findFirst()
                .orElseGet(apiKeyService::generateNewApiKey);
    }

    public Url deactivateUrl(Url url) {
        url.setActive(false);
        return url;
    }

}
