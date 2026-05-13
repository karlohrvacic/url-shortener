package cc.hrva.urlshortener.model;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UrlTest {

    @Test
    void shouldIncrementVisitsOnVisit() {
        final var url = Url.builder().visits(5L).build();

        url.onVisit();

        assertThat(url.getVisits()).isEqualTo(6L);
        assertThat(url.getLastAccessed()).isNotNull();
    }

    @Test
    void shouldDeactivateUrlWhenVisitLimitReached() {
        final var url = Url.builder().visits(9L).visitLimit(10L).active(true).build();

        url.onVisit();

        assertThat(url.getVisits()).isEqualTo(10L);
        assertThat(url.isActive()).isFalse();
    }

    @Test
    void shouldNotDeactivateUrlWhenVisitLimitNotReached() {
        final var url = Url.builder().visits(5L).visitLimit(10L).active(true).build();

        url.onVisit();

        assertThat(url.getVisits()).isEqualTo(6L);
        assertThat(url.isActive()).isTrue();
    }

    @Test
    void shouldNotDeactivateWhenNoVisitLimit() {
        final var url = Url.builder().visits(100L).visitLimit(null).active(true).build();

        url.onVisit();

        assertThat(url.getVisits()).isEqualTo(101L);
        assertThat(url.isActive()).isTrue();
    }

    @Test
    void shouldClearForAnonymousUser() {
        final var owner = User.builder().id(1L).build();
        final var apiKey = ApiKey.builder().id(1L).build();
        final var url = Url.builder().owner(owner).apiKey(apiKey).build();

        url.clearForAnonymousUser();

        assertThat(url.getOwner()).isNull();
        assertThat(url.getApiKey()).isNull();
    }

    @Test
    void shouldInitializeOnCreate() {
        final var url = new Url();
        url.onCreate();

        assertThat(url.getVisits()).isEqualTo(0L);
        assertThat(url.getCreateDate()).isNotNull();
        assertThat(url.isActive()).isTrue();
    }

    @Test
    void shouldVerifyUrlValidityAndDeactivate() {
        final var url = Url.builder().visits(10L).visitLimit(10L).active(true).build();

        url.verifyUrlValidity(url);

        assertThat(url.isActive()).isFalse();
    }

    @Test
    void shouldVerifyUrlValidityAndKeepActive() {
        final var url = Url.builder().visits(5L).visitLimit(10L).active(true).build();

        url.verifyUrlValidity(url);

        assertThat(url.isActive()).isTrue();
    }
}
