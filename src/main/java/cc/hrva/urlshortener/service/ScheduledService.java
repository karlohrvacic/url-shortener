package cc.hrva.urlshortener.service;

public interface ScheduledService {

    void cleanupIpAddresses();
    void deactivateExpiredUrls();
    void deactivateExpiredApiKeys();
    void deactivateUnusedUserAccounts();
    void cleanupExpiredPasswordResetTokens();
    void notifyExpiringUrls();

}
