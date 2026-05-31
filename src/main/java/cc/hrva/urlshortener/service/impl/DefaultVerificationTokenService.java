package cc.hrva.urlshortener.service.impl;

import cc.hrva.urlshortener.configuration.properties.AppProperties;
import cc.hrva.urlshortener.exception.ApiException;
import cc.hrva.urlshortener.model.User;
import cc.hrva.urlshortener.model.VerificationToken;
import cc.hrva.urlshortener.repository.VerificationTokenRepository;
import cc.hrva.urlshortener.service.VerificationTokenService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultVerificationTokenService implements VerificationTokenService {

    private final AppProperties appProperties;
    private final VerificationTokenRepository verificationTokenRepository;

    @Override
    public VerificationToken createTokenForUser(final User user) {
        return verificationTokenRepository.save(VerificationToken.builder()
                .user(user)
                .expirationDate(LocalDateTime.now().plusHours(appProperties.getVerificationTokenExpirationInHours()))
                .build());
    }

    @Override
    public VerificationToken validateToken(final String token) {
        final var verificationToken = verificationTokenRepository.findByTokenAndActiveTrue(token)
                .orElseThrow(() -> new ApiException("Verification link is invalid or already used"));

        if (verificationToken.getExpirationDate().isBefore(LocalDateTime.now())) {
            throw new ApiException("Verification link has expired");
        }

        return verificationToken;
    }

    @Override
    @Transactional
    public void deactivateAndSaveToken(final VerificationToken token) {
        token.setActive(false);
        verificationTokenRepository.save(token);
    }

    @Override
    @Transactional
    public void deactivateActiveTokensForUser(final User user) {
        final var tokens = verificationTokenRepository.findByUserAndActiveTrue(user).stream()
                .map(token -> {
                    token.setActive(false);
                    return token;
                })
                .toList();

        verificationTokenRepository.saveAll(tokens);
        if (!tokens.isEmpty()) {
            log.info("Deactivated {} verification tokens for user id={}", tokens.size(), user.getId());
        }
    }

}
