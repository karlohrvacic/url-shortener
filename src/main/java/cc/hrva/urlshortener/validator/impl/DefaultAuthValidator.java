package cc.hrva.urlshortener.validator.impl;

import lombok.RequiredArgsConstructor;
import cc.hrva.urlshortener.exception.NoAuthorizationException;
import cc.hrva.urlshortener.model.User;
import cc.hrva.urlshortener.validator.AuthValidator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultAuthValidator implements AuthValidator {

    private final PasswordEncoder passwordEncoder;

    @Override
    public void passwordMatchesCurrentPassword(final User user, final String password) {
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new NoAuthorizationException("Current password is incorrect");
        }
    }

}
