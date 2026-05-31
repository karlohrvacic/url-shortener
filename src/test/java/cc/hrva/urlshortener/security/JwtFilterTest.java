package cc.hrva.urlshortener.security;

import java.util.Optional;
import cc.hrva.urlshortener.model.User;
import cc.hrva.urlshortener.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    private JwtFilter jwtFilter;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        this.jwtFilter = new JwtFilter(tokenProvider, userRepository);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticateActiveUser() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer jwt");
        when(tokenProvider.validateToken("jwt")).thenReturn(true);
        when(tokenProvider.getAuthentication("jwt"))
                .thenReturn(new UsernamePasswordAuthenticationToken("test@example.com", "jwt"));
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(User.builder().id(1L).active(true).build()));

        jwtFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotAuthenticateInactiveUser() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer jwt");
        when(tokenProvider.validateToken("jwt")).thenReturn(true);
        when(tokenProvider.getAuthentication("jwt"))
                .thenReturn(new UsernamePasswordAuthenticationToken("evil@example.com", "jwt"));
        when(userRepository.findByEmail("evil@example.com"))
                .thenReturn(Optional.of(User.builder().id(1L).active(false).build()));

        jwtFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotAuthenticateWhenNoToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
