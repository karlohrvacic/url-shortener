package cc.hrva.urlshortener.dto;

import java.time.LocalDateTime;
import cc.hrva.urlshortener.model.ApiKey;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiKeyResponseTest {

    private ApiKey.ApiKeyBuilder baseKey() {
        return ApiKey.builder()
                .id(1L)
                .key("k")
                .apiCallsLimit(100L)
                .apiCallsUsed(10L)
                .createDate(LocalDateTime.now())
                .expirationDate(LocalDateTime.now().plusDays(1));
    }

    @Test
    void shouldBeActive() {
        assertThat(ApiKeyResponse.from(baseKey().active(true).build()).status()).isEqualTo("ACTIVE");
    }

    @Test
    void shouldBeLimitReached() {
        final var key = baseKey().active(false).apiCallsUsed(100L).build();
        assertThat(ApiKeyResponse.from(key).status()).isEqualTo("LIMIT_REACHED");
    }

    @Test
    void shouldBeExpired() {
        final var key = baseKey().active(false).expirationDate(LocalDateTime.now().minusDays(1)).build();
        assertThat(ApiKeyResponse.from(key).status()).isEqualTo("EXPIRED");
    }

    @Test
    void shouldBeRevoked() {
        assertThat(ApiKeyResponse.from(baseKey().active(false).build()).status()).isEqualTo("REVOKED");
    }
}
