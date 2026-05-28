package cc.hrva.urlshortener.validator.impl;

import cc.hrva.urlshortener.configuration.properties.AppProperties;
import cc.hrva.urlshortener.exception.ApiException;
import cc.hrva.urlshortener.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import cc.hrva.urlshortener.exception.EmailExistsException;
import cc.hrva.urlshortener.validator.UserValidator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultUserValidator implements UserValidator {

    private final AppProperties appProperties;
    private final UserRepository userRepository;

    @Override
    public void checkEmailUniqueness(final String email) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailExistsException("An account with this email already exists");
        }
    }

    @Override
    public void checkRegistrationEnabled() {
        if (!appProperties.isRegistrationEnabled()) {
            throw new ApiException("Registrations are currently disabled");
        }
    }

}
