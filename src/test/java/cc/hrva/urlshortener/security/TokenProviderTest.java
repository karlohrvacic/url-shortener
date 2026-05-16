package cc.hrva.urlshortener.security;

import cc.hrva.urlshortener.configuration.properties.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenProviderTest {

    private TokenProvider tokenProvider;

    @Mock
    private AppProperties appProperties;

    @BeforeEach
    void setUp() {
        when(appProperties.getJwtBase64Secret()).thenReturn("d2hhdGV2ZXIgeW91IGRvLCBkbyBub3QgdXNlIHRoaXMgaW4gcHJvZHVjdGlvbiEgVGhpcyBpcyBhIHNlY3JldCBrZXkhISE=");
        when(appProperties.getJwtTokenValiditySeconds()).thenReturn(86400L);
        this.tokenProvider = new TokenProvider(appProperties);
        this.tokenProvider.init();
    }

    @Test
    void shouldCreateAndValidateToken() {
        final var authentication = new UsernamePasswordAuthenticationToken(
                "test@example.com",
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        final var token = tokenProvider.createToken(authentication);

        assertThat(token).isNotNull();
        assertThat(tokenProvider.validateToken(token)).isTrue();
    }

    @Test
    void shouldGetAuthenticationFromToken() {
        final var authentication = new UsernamePasswordAuthenticationToken(
                "test@example.com",
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        final var token = tokenProvider.createToken(authentication);

        final var result = tokenProvider.getAuthentication(token);

        assertThat(result.getName()).isEqualTo("test@example.com");
        assertThat(result.getAuthorities()).hasSize(1);
        assertThat(result.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
    }

    @Test
    void shouldInvalidateMalformedToken() {
        assertThat(tokenProvider.validateToken("invalid-token")).isFalse();
    }

    @Test
    void shouldInvalidateEmptyToken() {
        assertThat(tokenProvider.validateToken("")).isFalse();
    }
}
