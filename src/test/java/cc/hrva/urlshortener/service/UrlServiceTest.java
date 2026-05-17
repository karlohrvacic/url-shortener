package cc.hrva.urlshortener.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import tools.jackson.databind.ObjectMapper;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import cc.hrva.urlshortener.configuration.properties.AppProperties;
import cc.hrva.urlshortener.converter.CreateUrlToUrlConverter;
import cc.hrva.urlshortener.converter.UrlToPeekUrlConverter;
import cc.hrva.urlshortener.converter.UrlUpdateDtoToUrlConverter;
import cc.hrva.urlshortener.dto.CreateUrlDto;
import cc.hrva.urlshortener.dto.UrlResponse;
import cc.hrva.urlshortener.dto.UrlSearchDto;
import cc.hrva.urlshortener.model.ApiKey;
import cc.hrva.urlshortener.model.Url;
import cc.hrva.urlshortener.model.User;
import cc.hrva.urlshortener.repository.UrlRepository;
import cc.hrva.urlshortener.service.impl.DefaultUrlService;
import cc.hrva.urlshortener.validator.ApiKeyValidator;
import cc.hrva.urlshortener.validator.UrlValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    private UrlService urlService;

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private ApiKeyService apiKeyService;

    @Mock
    private UserService userService;

    @Mock
    private UrlValidator urlValidator;

    @Mock
    private ApiKeyValidator apiKeyValidator;

    @Mock
    private AppProperties appProperties;

    @Mock
    private IPAddressService ipAddressService;

    @Mock
    private UrlUpdateDtoToUrlConverter urlUpdateDtoToUrlConverter;

    @Mock
    private TaskExecutor taskExecutor;

    @Mock
    private CreateUrlToUrlConverter createUrlToUrlConverter;

    @Mock
    private UrlToPeekUrlConverter urlToPeekUrlConverter;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        this.urlService = new DefaultUrlService(
                userService,
                urlValidator,
                apiKeyService,
                urlRepository,
                appProperties,
                apiKeyValidator,
                ipAddressService,
                taskExecutor,
                urlToPeekUrlConverter,
                createUrlToUrlConverter,
                urlUpdateDtoToUrlConverter,
                cacheManager,
                objectMapper
        );
    }

    @Test
    void shouldSaveUrlRandomShortUrl() {
        final var createUrlDto = CreateUrlDto.builder().longUrl("long").build();
        final var url = Url.builder().longUrl("long").build();

        when(appProperties.getShortUrlLength()).thenReturn(1L);
        when(urlRepository.save(url)).thenReturn(url);
        when(createUrlToUrlConverter.convert(createUrlDto)).thenReturn(url);

        final var result = urlService.saveUrlRouting(createUrlDto);
        assertThat(result.longUrl()).isEqualTo("long");

        verify(urlValidator).checkIfShortUrlIsUnique(url.getShortUrl());
        verify(urlValidator).longUrlInUrl(url);
    }

    @Test
    void shouldReturnSavedUrlRandomShortUrl() {
        final var createUrlDto = CreateUrlDto.builder().longUrl("long").build();
        final var url = Url.builder().longUrl("long").build();
        final var existingLongUrl = Url.builder().longUrl("long").active(true).build();

        when(createUrlToUrlConverter.convert(createUrlDto)).thenReturn(url);
        when(userService.getUserFromToken()).thenReturn(null);
        when(urlRepository.existsUrlByLongUrlAndActiveTrueAndOwnerIsNull(url.getLongUrl())).thenReturn(true);
        when(urlRepository.findByLongUrlAndActiveTrue(url.getLongUrl())).thenReturn(Optional.ofNullable(existingLongUrl));

        final var result = urlService.saveUrlRouting(createUrlDto);
        assertThat(result.longUrl()).isEqualTo("long");
        assertThat(result.active()).isTrue();
    }

    @Test
    void shouldSaveUrlWithApiKey() {
        final var createUrlDto = CreateUrlDto.builder().shortUrl("").build();
        final var url = Url.builder().shortUrl("").build();
        final var api = "apikey";
        final var apiKey = ApiKey.builder().build();

        when(urlRepository.save(url)).thenReturn(url);
        when(apiKeyService.fetchApiKeyByKey(api)).thenReturn(apiKey);
        when(createUrlToUrlConverter.convert(createUrlDto)).thenReturn(url);

        final var result = urlService.saveUrlWithApiKey(createUrlDto, api);
        assertThat(result).isInstanceOf(UrlResponse.class);

        verify(apiKeyValidator).apiKeyExistsByKeyAndIsValid(api);
        verify(apiKeyService).apiKeyUseAction(any(ApiKey.class));
    }

    @Test
    void shouldFetchUrls() {
        final var apiKey = "apikey";
        final var user = User.builder().build();
        final var key = ApiKey.builder().owner(user).build();
        final var pageable = PageRequest.of(0, 20);
        final var url = Url.builder().build();
        final var urls = new PageImpl<>(Collections.singletonList(url));

        when(apiKeyService.fetchApiKeyByKey(apiKey)).thenReturn(key);
        when(urlRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(urls);

        final var result = urlService.getAllMyUrls(apiKey, pageable, new UrlSearchDto());
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void shouldFetchUrlsWithSearchFilter() {
        final var apiKey = "apikey";
        final var user = User.builder().build();
        final var key = ApiKey.builder().owner(user).build();
        final var pageable = PageRequest.of(0, 20);
        final var url = Url.builder().longUrl("https://example.com").build();
        final var urls = new PageImpl<>(Collections.singletonList(url));
        final var search = new UrlSearchDto();
        search.setSearch("example");

        when(apiKeyService.fetchApiKeyByKey(apiKey)).thenReturn(key);
        when(urlRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(urls);

        final var result = urlService.getAllMyUrls(apiKey, pageable, search);
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().longUrl()).isEqualTo("https://example.com");
    }

    @Test
    void shouldFetchUrlsWithActiveFilter() {
        final var apiKey = "apikey";
        final var user = User.builder().build();
        final var key = ApiKey.builder().owner(user).build();
        final var pageable = PageRequest.of(0, 20);
        final var url = Url.builder().active(true).build();
        final var urls = new PageImpl<>(Collections.singletonList(url));
        final var search = new UrlSearchDto();
        search.setActive(true);

        when(apiKeyService.fetchApiKeyByKey(apiKey)).thenReturn(key);
        when(urlRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(urls);

        final var result = urlService.getAllMyUrls(apiKey, pageable, search);
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().active()).isTrue();
    }

    @Test
    void shouldFetchUrlsWithExpiredFilter() {
        final var apiKey = "apikey";
        final var user = User.builder().build();
        final var key = ApiKey.builder().owner(user).build();
        final var pageable = PageRequest.of(0, 20);
        final var url = Url.builder().expirationDate(LocalDateTime.now().minusDays(1)).build();
        final var urls = new PageImpl<>(Collections.singletonList(url));
        final var search = new UrlSearchDto();
        search.setExpired(true);

        when(apiKeyService.fetchApiKeyByKey(apiKey)).thenReturn(key);
        when(urlRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(urls);

        final var result = urlService.getAllMyUrls(apiKey, pageable, search);
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void shouldFetchUrlsWithDateRangeFilter() {
        final var apiKey = "apikey";
        final var user = User.builder().build();
        final var key = ApiKey.builder().owner(user).build();
        final var pageable = PageRequest.of(0, 20);
        final var url = Url.builder().createDate(LocalDateTime.now()).build();
        final var urls = new PageImpl<>(Collections.singletonList(url));
        final var search = new UrlSearchDto();
        search.setDateFrom(LocalDateTime.now().minusDays(7));
        search.setDateTo(LocalDateTime.now().plusDays(1));

        when(apiKeyService.fetchApiKeyByKey(apiKey)).thenReturn(key);
        when(urlRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(urls);

        final var result = urlService.getAllMyUrls(apiKey, pageable, search);
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void shouldSaveUrlWithApiKeyWithFirstApiKeyForLoggedInUser() {
        final var createUrlDto = CreateUrlDto.builder().shortUrl("").build();
        final var url = Url.builder().shortUrl("").build();
        final var apiKey = ApiKey.builder().id(1L).key("key").apiCallsUsed(0L).apiCallsLimit(10L).active(true).build();
        final var user = User.builder().id(1L).apiKeys(Collections.singletonList(apiKey)).build();

        when(createUrlToUrlConverter.convert(createUrlDto)).thenReturn(url);
        when(userService.getUserFromToken()).thenReturn(user);
        when(urlRepository.save(url)).thenReturn(url);
        when(appProperties.getShortUrlLength()).thenReturn(1L);

        final var result = urlService.saveUrlWithApiKey(createUrlDto, null);
        assertThat(result).isInstanceOf(UrlResponse.class);

        verify(apiKeyValidator).apiKeyExistsByKeyAndIsValid("key");
        verify(apiKeyService).apiKeyUseAction(any(ApiKey.class));
    }

    @Test
    void shouldActivateUrl() {
        final var url = Url.builder().id(1L).active(false).build();
        final var activatedUrl = Url.builder().id(1L).active(true).build();
        when(urlRepository.findById(1L)).thenReturn(Optional.of(url));
        when(urlRepository.save(any(Url.class))).thenReturn(activatedUrl);

        final var result = urlService.activateUrl(1L);
        assertThat(result.active()).isTrue();
    }

}
