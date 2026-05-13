package cc.hrva.urlshortener.service;

import java.util.Optional;
import cc.hrva.urlshortener.exception.UserDoesntExistException;
import cc.hrva.urlshortener.model.codebook.Authorities;
import cc.hrva.urlshortener.repository.AuthoritiesRepository;
import cc.hrva.urlshortener.service.impl.DefaultAuthoritiesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultAuthoritiesServiceTest {

    private AuthoritiesService authoritiesService;

    @Mock
    private AuthoritiesRepository authoritiesRepository;

    @BeforeEach
    void setUp() {
        this.authoritiesService = new DefaultAuthoritiesService(authoritiesRepository);
    }

    @Test
    void shouldGetDefaultAuthorityByName() {
        final var authority = Authorities.builder().id(1L).name("ROLE_USER").build();

        when(authoritiesRepository.findByName("ROLE_USER")).thenReturn(Optional.of(authority));
        when(authoritiesRepository.findById(1L)).thenReturn(Optional.of(authority));

        assertThat(authoritiesService.getDefaultAuthority()).isEqualTo(authority);
    }

    @Test
    void shouldGetDefaultAuthorityByIdWhenNameNotFound() {
        final var authority = Authorities.builder().id(1L).name("ROLE_USER").build();

        when(authoritiesRepository.findByName("ROLE_USER")).thenReturn(Optional.empty());
        when(authoritiesRepository.findById(1L)).thenReturn(Optional.of(authority));

        assertThat(authoritiesService.getDefaultAuthority()).isEqualTo(authority);
    }

    @Test
    void shouldThrowWhenAuthorityNotFound() {
        when(authoritiesRepository.findByName("ROLE_USER")).thenReturn(Optional.empty());
        when(authoritiesRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authoritiesService.getDefaultAuthority())
                .isInstanceOf(UserDoesntExistException.class)
                .hasMessage("Authority not found");
    }
}
