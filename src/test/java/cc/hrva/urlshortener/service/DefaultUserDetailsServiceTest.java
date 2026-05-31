package cc.hrva.urlshortener.service;

import java.util.Collections;
import java.util.Optional;
import cc.hrva.urlshortener.model.User;
import cc.hrva.urlshortener.model.codebook.Authorities;
import cc.hrva.urlshortener.repository.UserRepository;
import cc.hrva.urlshortener.service.impl.DefaultUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultUserDetailsServiceTest {

    private DefaultUserDetailsService userDetailsService;

    @Mock
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        this.userDetailsService = new DefaultUserDetailsService(userRepository);
    }

    private User userWithActive(final Boolean active) {
        return User.builder()
                .id(1L)
                .email("test@example.com")
                .password("encoded")
                .active(active)
                .authorities(Collections.singletonList(Authorities.builder().id(1L).name("ROLE_USER").build()))
                .build();
    }

    @Test
    void shouldBeEnabledWhenUserActive() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(userWithActive(true)));

        final var details = userDetailsService.loadUserByUsername("test@example.com");

        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    void shouldBeDisabledWhenUserInactive() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(userWithActive(false)));

        final var details = userDetailsService.loadUserByUsername("test@example.com");

        assertThat(details.isEnabled()).isFalse();
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("missing@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
