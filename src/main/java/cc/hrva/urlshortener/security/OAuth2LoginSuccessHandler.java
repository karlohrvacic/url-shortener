package cc.hrva.urlshortener.security;

import cc.hrva.urlshortener.configuration.properties.AppProperties;
import cc.hrva.urlshortener.model.User;
import cc.hrva.urlshortener.repository.UserRepository;
import cc.hrva.urlshortener.service.AuthoritiesService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);

    private final UserRepository userRepository;
    private final AuthoritiesService authoritiesService;
    private final TokenProvider tokenProvider;
    private final AppProperties appProperties;

    public OAuth2LoginSuccessHandler(
            final UserRepository userRepository,
            final AuthoritiesService authoritiesService,
            final TokenProvider tokenProvider,
            final AppProperties appProperties) {
        this.userRepository = userRepository;
        this.authoritiesService = authoritiesService;
        this.tokenProvider = tokenProvider;
        this.appProperties = appProperties;
    }

    @Override
    @Transactional
    public void onAuthenticationSuccess(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Authentication authentication) throws IOException {

        final var oAuth2User = (OAuth2User) authentication.getPrincipal();
        final var email = extractEmail(oAuth2User);

        if (email == null) {
            log.warn("OAuth2 login failed: no email returned from provider");
            response.sendRedirect(appProperties.getFrontendUrl() + "/auth/callback?error=no_email");
            return;
        }

        final var provider = request.getRequestURI().contains("google") ? "google" : "github";
        final var user = userRepository.findByEmail(email)
                .orElseGet(() -> createOAuth2User(email, provider));

        final var springAuth = new UsernamePasswordAuthenticationToken(
                user.getEmail(), null,
                user.getAuthorities().stream()
                        .map(a -> new SimpleGrantedAuthority(a.getName()))
                        .toList());

        final var jwt = tokenProvider.createToken(springAuth);
        final var redirectUrl = String.format("%s/auth/callback?token=%s",
                appProperties.getFrontendUrl(),
                URLEncoder.encode(jwt, StandardCharsets.UTF_8));

        log.info("OAuth2 login success email={}", email);
        response.sendRedirect(redirectUrl);
    }

    private String extractEmail(final OAuth2User oAuth2User) {
        final var email = oAuth2User.getAttribute("email");
        if (email instanceof String) {
            return (String) email;
        }
        // GitHub may not return email directly — check the login attribute
        return oAuth2User.getAttribute("login");
    }

    private User createOAuth2User(final String email, final String provider) {
        final var newUser = User.builder()
                .email(email)
                .password(UUID.randomUUID().toString())
                .authProvider(provider)
                .authorities(List.of(authoritiesService.getDefaultAuthority()))
                .apiKeySlots(appProperties.getUserApiKeySlots())
                .build();

        final var saved = userRepository.save(newUser);
        saved.setActive(true);
        final var activated = userRepository.save(saved);

        log.info("Created OAuth2 user email={}", email);
        return activated;
    }

}
