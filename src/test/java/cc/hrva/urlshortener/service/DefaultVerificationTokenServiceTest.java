package cc.hrva.urlshortener.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import cc.hrva.urlshortener.configuration.properties.AppProperties;
import cc.hrva.urlshortener.exception.ApiException;
import cc.hrva.urlshortener.model.User;
import cc.hrva.urlshortener.model.VerificationToken;
import cc.hrva.urlshortener.repository.VerificationTokenRepository;
import cc.hrva.urlshortener.service.impl.DefaultVerificationTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultVerificationTokenServiceTest {

    private VerificationTokenService service;

    @Mock
    private AppProperties appProperties;

    @Mock
    private VerificationTokenRepository verificationTokenRepository;

    @BeforeEach
    void setUp() {
        this.service = new DefaultVerificationTokenService(appProperties, verificationTokenRepository);
    }

    @Test
    void shouldCreateTokenForUser() {
        final var user = User.builder().id(1L).build();
        when(appProperties.getVerificationTokenExpirationInHours()).thenReturn(48L);
        when(verificationTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        final var token = service.createTokenForUser(user);

        assertThat(token.getUser()).isEqualTo(user);
        assertThat(token.getExpirationDate()).isAfter(LocalDateTime.now());
    }

    @Test
    void shouldValidateActiveToken() {
        final var token = VerificationToken.builder()
                .id(1L).active(true).expirationDate(LocalDateTime.now().plusHours(1)).build();
        when(verificationTokenRepository.findByTokenAndActiveTrue("t")).thenReturn(Optional.of(token));

        assertThat(service.validateToken("t")).isEqualTo(token);
    }

    @Test
    void shouldFailValidateWhenTokenMissing() {
        when(verificationTokenRepository.findByTokenAndActiveTrue("t")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validateToken("t"))
                .isInstanceOf(ApiException.class)
                .hasMessage("Verification link is invalid or already used");
    }

    @Test
    void shouldFailValidateWhenTokenExpired() {
        final var token = VerificationToken.builder()
                .id(1L).active(true).expirationDate(LocalDateTime.now().minusHours(1)).build();
        when(verificationTokenRepository.findByTokenAndActiveTrue("t")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.validateToken("t"))
                .isInstanceOf(ApiException.class)
                .hasMessage("Verification link has expired");
    }

    @Test
    void shouldDeactivateActiveTokensForUser() {
        final var user = User.builder().id(1L).build();
        final var token = VerificationToken.builder().id(1L).active(true).build();
        when(verificationTokenRepository.findByUserAndActiveTrue(user)).thenReturn(Collections.singletonList(token));

        service.deactivateActiveTokensForUser(user);

        assertThat(token.isActive()).isFalse();
        verify(verificationTokenRepository).saveAll(any());
    }
}
