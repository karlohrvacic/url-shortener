package cc.hrva.urlshortener.service.impl;

import cc.hrva.urlshortener.configuration.properties.AppProperties;
import cc.hrva.urlshortener.exception.NoAuthorizationException;
import cc.hrva.urlshortener.repository.ResetTokenRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cc.hrva.urlshortener.model.ResetToken;
import cc.hrva.urlshortener.model.User;
import cc.hrva.urlshortener.service.ResetTokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultResetTokenService implements ResetTokenService {

    private final AppProperties appProperties;
    private final ResetTokenRepository resetTokenRepository;

    @Override
    public void deactivateExpiredPasswordResetTokens() {
        deactivateResetTokens(resetTokenRepository.findByExpirationDateIsLessThanEqualAndActiveTrue(LocalDateTime.now()));
    }

    @Override
    public void deactivateActiveResetTokenIfExists(final User user) {
        deactivateResetTokens(resetTokenRepository.findByUserAndActiveTrue(user));
    }

    @Override
    public void deleteExpiredPasswordResetTokens() {
        final var resetTokens = resetTokenRepository.findByExpirationDateIsLessThanEqualAndActiveFalse(
                LocalDateTime.now().minusHours(appProperties.getIpRetentionDurationInHours()));

        resetTokenRepository.deleteAll(resetTokens);
        if (!resetTokens.isEmpty()) log.info("Deleted {} reset tokens", resetTokens.size());
    }

    @Override
    public ResetToken getResetTokenFromUserAndToken(final User user, final String token) {
        return resetTokenRepository.findResetTokenByUserAndTokenAndActiveTrue(user, token)
                .orElseThrow(() -> new NoAuthorizationException("Invalid credentials"));
    }

    @Override
    @Transactional
    public void deactivateAndSaveToken(final ResetToken token) {
        token.setActive(false);
        resetTokenRepository.save(token);
    }

    @Override
    public ResetToken createTokenForUser(final User user) {
        return resetTokenRepository.save(ResetToken.builder()
                .user(user)
                .expirationDate(LocalDateTime.now().plusHours(appProperties.getResetTokenExpirationInHours()))
                .build());
    }

    private void deactivateResetTokens(List<ResetToken> resetTokens) {
        resetTokens = resetTokens.stream()
                .map(resetToken -> {
                    resetToken.setActive(false);
                    return resetToken;
                })
                .toList();

        resetTokenRepository.saveAll(resetTokens);
        if (!resetTokens.isEmpty()) log.info("Deactivated {} reset tokens", resetTokens.size());
    }

}
