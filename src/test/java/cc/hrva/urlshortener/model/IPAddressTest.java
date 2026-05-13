package cc.hrva.urlshortener.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IPAddressTest {

    @Test
    void shouldIncrementVisitsOnAddVisit() {
        final var ipAddress = IPAddress.builder().visits(5L).build();

        ipAddress.addVisit();

        assertThat(ipAddress.getVisits()).isEqualTo(6L);
    }

    @Test
    void shouldInitializeOnCreate() {
        final var ipAddress = new IPAddress();
        ipAddress.onCreate();

        assertThat(ipAddress.getCreateDate()).isNotNull();
        assertThat(ipAddress.getVisits()).isEqualTo(1L);
        assertThat(ipAddress.isActive()).isTrue();
    }
}
