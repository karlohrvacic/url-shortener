package cc.hrva.urlshortener.validator.impl;

import cc.hrva.urlshortener.configuration.properties.AppProperties;
import cc.hrva.urlshortener.exception.*;
import cc.hrva.urlshortener.model.Url;
import cc.hrva.urlshortener.repository.UrlRepository;
import cc.hrva.urlshortener.service.SafeBrowsingService;
import cc.hrva.urlshortener.service.UserService;
import cc.hrva.urlshortener.validator.UrlValidator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DefaultUrlValidator implements UrlValidator {

    private final UserService userService;
    private final UrlRepository urlRepository;
    private final AppProperties appProperties;
    private final SafeBrowsingService safeBrowsingService;

    private static final List<String> RESERVED_PREFIXES = List.of("api");

    @Override
    public void longUrlInUrl(final Url url) {
        if (url.getLongUrl() == null) {
            throw new LongUrlNotSpecifiedException("URL for shortening is not specified");
        }
    }

    @Override
    public void checkIfShortUrlIsUnique(final String shortUrl) {
        if (urlRepository.existsUrlByShortUrlAndActiveTrue(shortUrl)) {
            throw new ShortUrlAlreadyExistsException("Short URL is already in use");
        }
    }

    @Override
    public void checkIfShortUrlIsReserved(final String shortUrl) {
        if (shortUrl != null && RESERVED_PREFIXES.stream().anyMatch(shortUrl::startsWith)) {
            throw new ShortUrlAlreadyExistsException("Short URL can't start with reserved prefix: " + shortUrl);
        }
    }

    @Override
    public void verifyUserAdminOrOwner(final Url url) {
        final var currentUser = userService.getUserFromToken();
        final var isCurrentUserAdmin = currentUser.getAuthorities().stream()
                .anyMatch(authorities -> authorities.getName().equals("ROLE_ADMIN"));

        if (!url.getOwner().equals(currentUser) && !isCurrentUserAdmin) {
            throw new NoAuthorizationException("You don't have authorization for this action");
        }
    }

    @Override
    public void checkIfUrlExpirationDateIsInThePast(final Url url) {
        if (url.getExpirationDate() != null && LocalDateTime.now().isAfter(url.getExpirationDate())) {
            throw new UrlValidationException("Expiration date can't be in the past");
        }
    }

    @Override
    public void checkIfUrlSafe(final Url url) {
        if (!safeBrowsingService.checkUrlsForThreats(url.getLongUrl()).isEmpty()) {
            throw new UrlValidationException("Bad URL detected");
        }
    }

    @Override
    public void checkIfAnonymousUrlCreationEnabled() {
        if (!appProperties.isAnonymousUrlCreationEnabled()) {
            throw new ApiException("Url creation for anonymous users is currently disabled");
        }
    }

}

