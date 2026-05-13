package cc.hrva.urlshortener.service;

import cc.hrva.urlshortener.service.impl.DefaultScheduledService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DefaultScheduledServiceTest {

    private ScheduledService scheduledService;

    @Mock
    private UrlService urlService;

    @Mock
    private UserService userService;

    @Mock
    private ApiKeyService apiKeyService;

    @Mock
    private IPAddressService ipAddressService;

    @Mock
    private ResetTokenService resetTokenService;

    @BeforeEach
    void setUp() {
        this.scheduledService = new DefaultScheduledService(urlService, userService, apiKeyService, ipAddressService, resetTokenService);
    }

    @Test
    void shouldDeactivateDeprecatedIps() {
        scheduledService.deactivateDeprecatedIps();
        verify(ipAddressService).deactivateDeprecatedIps();
    }

    @Test
    void shouldDeleteDeactivatedIps() {
        scheduledService.deleteDeactivatedIps();
        verify(ipAddressService).deleteDeactivatedIps();
    }

    @Test
    void shouldDeactivateExpiredPasswordResetTokens() {
        scheduledService.deactivateExpiredPasswordResetTokens();
        verify(resetTokenService).deactivateExpiredPasswordResetTokens();
    }

    @Test
    void shouldDeleteExpiredPasswordResetTokens() {
        scheduledService.deleteExpiredPasswordResetTokens();
        verify(resetTokenService).deleteExpiredPasswordResetTokens();
    }

    @Test
    void shouldDeactivateUnusedUserAccounts() {
        scheduledService.deactivateUnusedUserAccounts();
        verify(userService).deactivateUnusedUserAccounts();
    }

    @Test
    void shouldDeactivateExpiredApiKeys() {
        scheduledService.deactivateExpiredApiKeys();
        verify(apiKeyService).deactivateExpired();
    }

    @Test
    void shouldDeactivateExpiredUrls() {
        scheduledService.deactivateExpiredUrls();
        verify(urlService).deactivateExpiredUrls();
    }
}
