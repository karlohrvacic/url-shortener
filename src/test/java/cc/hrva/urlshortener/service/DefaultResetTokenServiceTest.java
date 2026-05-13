package cc.hrva.urlshortener.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import cc.hrva.urlshortener.configuration.properties.AppProperties;
import cc.hrva.urlshortener.exception.NoAuthorizationException;
import cc.hrva.urlshortener.model.ResetToken;
import cc.hrva.urlshortener.model.User;
import cc.hrva.urlshortener.repository.ResetTokenRepository;
import cc.hrva.urlshortener.service.impl.DefaultResetTokenService;
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
class DefaultResetTokenServiceTest {

    private ResetTokenService resetTokenService;

    @Mock
    private AppProperties appProperties;

    @Mock
    private ResetTokenRepository resetTokenRepository;

    @BeforeEach
    void setUp() {
        this.resetTokenService = new DefaultResetTokenService(appProperties, resetTokenRepository);
    }

    @Test
    void shouldDeactivateExpiredPasswordResetTokens() {
        final var token = ResetToken.builder().id(1L).active(true).build();
        when(resetTokenRepository.findByExpirationDateIsLessThanEqualAndActiveTrue(any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(token));
        when(resetTokenRepository.saveAll(any())).thenReturn(Collections.singletonList(token));

        resetTokenService.deactivateExpiredPasswordResetTokens();

        assertThat(token.isActive()).isFalse();
    }

    @Test
    void shouldDeactivateActiveResetTokenIfExists() {
        final var user = User.builder().id(1L).build();
        final var token = ResetToken.builder().id(1L).active(true).build();
        when(resetTokenRepository.findByUserAndActiveTrue(user)).thenReturn(Collections.singletonList(token));
        when(resetTokenRepository.saveAll(any())).thenReturn(Collections.singletonList(token));

        resetTokenService.deactivateActiveResetTokenIfExists(user);

        assertThat(token.isActive()).isFalse();
    }

    @Test
    void shouldDeleteExpiredPasswordResetTokens() {
        final var token = ResetToken.builder().id(1L).active(false).build();
        when(appProperties.getIpRetentionDurationInHours()).thenReturn(24L);
        when(resetTokenRepository.findByExpirationDateIsLessThanEqualAndActiveFalse(any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(token));

        resetTokenService.deleteExpiredPasswordResetTokens();

        verify(resetTokenRepository).deleteAll(Collections.singletonList(token));
    }

    @Test
    void shouldGetResetTokenFromUserAndToken() {
        final var user = User.builder().id(1L).build();
        final var token = ResetToken.builder().id(1L).active(true).build();
        when(resetTokenRepository.findResetTokenByUserAndTokenAndActiveTrue(user, "token"))
                .thenReturn(Optional.of(token));

        final var result = resetTokenService.getResetTokenFromUserAndToken(user, "token");

        assertThat(result).isEqualTo(token);
    }

    @Test
    void shouldFailGetResetTokenWhenNotFound() {
        final var user = User.builder().id(1L).build();
        when(resetTokenRepository.findResetTokenByUserAndTokenAndActiveTrue(user, "token"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> resetTokenService.getResetTokenFromUserAndToken(user, "token"))
                .isInstanceOf(NoAuthorizationException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void shouldDeactivateAndSaveToken() {
        final var token = ResetToken.builder().id(1L).active(true).build();
        when(resetTokenRepository.save(token)).thenReturn(token);

        resetTokenService.deactivateAndSaveToken(token);

        assertThat(token.isActive()).isFalse();
        verify(resetTokenRepository).save(token);
    }

    @Test
    void shouldCreateTokenForUser() {
        final var user = User.builder().id(1L).build();
        when(appProperties.getResetTokenExpirationInHours()).thenReturn(24L);
        when(resetTokenRepository.save(any(ResetToken.class))).thenAnswer(inv -> inv.getArgument(0));

        final var token = resetTokenService.createTokenForUser(user);

        assertThat(token.getUser()).isEqualTo(user);
        assertThat(token.getExpirationDate()).isAfter(LocalDateTime.now());
    }
}
