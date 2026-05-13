package cc.hrva.urlshortener.service;

import java.time.LocalDateTime;
import java.util.Collections;
import cc.hrva.urlshortener.configuration.properties.AppProperties;
import cc.hrva.urlshortener.model.IPAddress;
import cc.hrva.urlshortener.model.Url;
import cc.hrva.urlshortener.repository.IPAddressRepository;
import cc.hrva.urlshortener.service.impl.DefaultIPAddressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultIPAddressServiceTest {

    private IPAddressService ipAddressService;

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private AppProperties appProperties;

    @Mock
    private IPAddressRepository ipAddressRepository;

    @BeforeEach
    void setUp() {
        this.ipAddressService = new DefaultIPAddressService(encoder, appProperties, ipAddressRepository);
    }

    @Test
    void shouldReturnTrueWhenUrlAlreadyVisitedByIP() {
        final var url = Url.builder().id(1L).build();
        final var ipAddress = IPAddress.builder().hashedIPAddress("hashedIP").visits(1L).build();

        when(ipAddressRepository.findByUrlAndActiveTrue(url)).thenReturn(Collections.singletonList(ipAddress));
        when(encoder.matches("192.168.1.1", "hashedIP")).thenReturn(true);
        when(ipAddressRepository.save(any(IPAddress.class))).thenAnswer(inv -> inv.getArgument(0));

        final var result = ipAddressService.urlAlreadyVisitedByIP(url, "192.168.1.1");

        assertThat(result).isTrue();
        verify(ipAddressRepository).save(any(IPAddress.class));
    }

    @Test
    void shouldReturnFalseAndSaveWhenUrlNotVisitedByIP() {
        final var url = Url.builder().id(1L).build();

        when(ipAddressRepository.findByUrlAndActiveTrue(url)).thenReturn(Collections.emptyList());
        when(encoder.encode("192.168.1.1")).thenReturn("hashedIP");
        when(ipAddressRepository.save(any(IPAddress.class))).thenAnswer(inv -> inv.getArgument(0));

        final var result = ipAddressService.urlAlreadyVisitedByIP(url, "192.168.1.1");

        assertThat(result).isFalse();
        verify(ipAddressRepository).save(any(IPAddress.class));
    }

    @Test
    void shouldSaveUrlVisitByIP() {
        final var url = Url.builder().id(1L).build();
        when(encoder.encode("192.168.1.1")).thenReturn("hashedIP");
        when(ipAddressRepository.save(any(IPAddress.class))).thenAnswer(inv -> inv.getArgument(0));

        ipAddressService.saveUrlVisitByIP(url, "192.168.1.1");

        verify(ipAddressRepository).save(any(IPAddress.class));
    }

    @Test
    void shouldDeactivateDeprecatedIps() {
        final var ipAddress = IPAddress.builder().id(1L).active(true).build();
        when(appProperties.getInactiveVisitIncrementPerIpInHours()).thenReturn(24L);
        when(ipAddressRepository.findByCreateDateIsLessThanEqualAndActiveTrue(any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(ipAddress));
        when(ipAddressRepository.saveAll(any())).thenReturn(Collections.singletonList(ipAddress));

        ipAddressService.deactivateDeprecatedIps();

        assertThat(ipAddress.isActive()).isFalse();
    }

    @Test
    void shouldDeleteDeactivatedIps() {
        final var ipAddress = IPAddress.builder().id(1L).active(false).build();
        when(appProperties.getIpRetentionDurationInHours()).thenReturn(24L);
        when(ipAddressRepository.findByCreateDateIsLessThanEqualAndActiveFalse(any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(ipAddress));

        ipAddressService.deleteDeactivatedIps();

        verify(ipAddressRepository).deleteAll(Collections.singletonList(ipAddress));
    }

    @Test
    void shouldDeleteRecordsForUrl() {
        final var url = Url.builder().id(1L).build();
        final var ipAddresses = Collections.singletonList(IPAddress.builder().id(1L).build());
        when(ipAddressRepository.findAllByUrl(url)).thenReturn(ipAddresses);

        ipAddressService.deleteRecordsForUrl(url);

        verify(ipAddressRepository).deleteAll(ipAddresses);
    }
}
