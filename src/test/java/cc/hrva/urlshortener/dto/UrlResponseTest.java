package cc.hrva.urlshortener.dto;

import java.time.LocalDateTime;
import cc.hrva.urlshortener.model.Url;
import cc.hrva.urlshortener.model.enums.ThreatType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UrlResponseTest {

    private Url.UrlBuilder baseUrl() {
        return Url.builder()
                .id(1L)
                .longUrl("https://example.com")
                .shortUrl("abc")
                .createDate(LocalDateTime.now())
                .expirationDate(LocalDateTime.now().plusDays(1))
                .visits(5L)
                .visitLimit(100L);
    }

    @Test
    void shouldBeActive() {
        assertThat(UrlResponse.from(baseUrl().active(true).build()).status()).isEqualTo("ACTIVE");
    }

    @Test
    void shouldBeBlockedWhenThreatPresent() {
        final var url = baseUrl().active(false).threatType(ThreatType.MALWARE).build();
        assertThat(UrlResponse.from(url).status()).isEqualTo("BLOCKED");
    }

    @Test
    void shouldBeLimitReached() {
        final var url = baseUrl().active(false).visits(100L).build();
        assertThat(UrlResponse.from(url).status()).isEqualTo("LIMIT_REACHED");
    }

    @Test
    void shouldBeExpired() {
        final var url = baseUrl().active(false).expirationDate(LocalDateTime.now().minusDays(1)).build();
        assertThat(UrlResponse.from(url).status()).isEqualTo("EXPIRED");
    }

    @Test
    void shouldBeDeactivated() {
        assertThat(UrlResponse.from(baseUrl().active(false).build()).status()).isEqualTo("DEACTIVATED");
    }
}
