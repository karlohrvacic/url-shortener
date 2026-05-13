package cc.hrva.urlshortener.service;

import java.util.Collections;
import java.util.Optional;
import cc.hrva.urlshortener.configuration.properties.AppProperties;
import cc.hrva.urlshortener.converter.CreateUrlToUrlConverter;
import cc.hrva.urlshortener.converter.UrlToPeekUrlConverter;
import cc.hrva.urlshortener.converter.UrlUpdateDtoToUrlConverter;
import cc.hrva.urlshortener.dto.CreateUrlDto;
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
                urlUpdateDtoToUrlConverter
        );
    }

    @Test
    void shouldSaveUrlRandomShortUrl() {
        final var createUrlDto = CreateUrlDto.builder().longUrl("long").build();
        final var url = Url.builder().longUrl("long").build();

        when(appProperties.getShortUrlLength()).thenReturn(1L);
        when(urlRepository.save(url)).thenReturn(url);
        when(createUrlToUrlConverter.convert(createUrlDto)).thenReturn(url);

        assertThat(urlService.saveUrlRouting(createUrlDto)).isEqualTo(url);

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

        assertThat(urlService.saveUrlRouting(createUrlDto)).isEqualTo(existingLongUrl);
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

        assertThat(urlService.saveUrlWithApiKey(createUrlDto, api)).isEqualTo(url);

        verify(apiKeyValidator).apiKeyExistsByKeyAndIsValid(api);
        verify(apiKeyService).apiKeyUseAction(any(ApiKey.class));
    }

    @Test
    void shouldFetchUrls() {
        final var apiKey = "apikey";
        final var user = User.builder().build();
        final var key = ApiKey.builder().owner(user).build();
        final var urls = Collections.singletonList(Url.builder().build());

        when(apiKeyService.fetchApiKeyByKey(apiKey)).thenReturn(key);
        when(urlRepository.findAllByOwner(user)).thenReturn(Optional.of(urls));

        assertThat(urlService.getAllMyUrls(apiKey)).isEqualTo(urls);
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

        assertThat(urlService.saveUrlWithApiKey(createUrlDto, null)).isEqualTo(url);

        verify(apiKeyValidator).apiKeyExistsByKeyAndIsValid("key");
        verify(apiKeyService).apiKeyUseAction(any(ApiKey.class));
    }

}
