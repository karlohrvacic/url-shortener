package cc.hrva.urlshortener.service.impl;

import cc.hrva.urlshortener.exception.UserNotFoundException;
import cc.hrva.urlshortener.repository.AuthoritiesRepository;
import lombok.RequiredArgsConstructor;
import cc.hrva.urlshortener.model.codebook.Authorities;
import cc.hrva.urlshortener.service.AuthoritiesService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefaultAuthoritiesService implements AuthoritiesService {

    private static final String DEFAULT_AUTHORITY_NAME = "ROLE_USER";

    private final AuthoritiesRepository authoritiesRepository;

    @Override
    public Authorities getDefaultAuthority() {
        return authoritiesRepository.findByName(DEFAULT_AUTHORITY_NAME)
            .orElse(authoritiesRepository.findById(1L).orElseThrow(() -> new UserNotFoundException("Authority not found")));
    }

}
