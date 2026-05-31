package cc.hrva.urlshortener.service;

import cc.hrva.urlshortener.model.User;
import cc.hrva.urlshortener.model.VerificationToken;

public interface VerificationTokenService {

    VerificationToken createTokenForUser(User user);
    VerificationToken validateToken(String token);
    void deactivateAndSaveToken(VerificationToken token);
    void deactivateActiveTokensForUser(User user);

}
