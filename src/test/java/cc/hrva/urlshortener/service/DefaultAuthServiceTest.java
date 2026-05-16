package cc.hrva.urlshortener.service;

import jakarta.servlet.http.HttpServletRequest;
import cc.hrva.urlshortener.configuration.properties.AppProperties;
import cc.hrva.urlshortener.security.JwtFilter;
import cc.hrva.urlshortener.security.TokenProvider;
import cc.hrva.urlshortener.converter.UserRegisterDtoToUserConverter;
import cc.hrva.urlshortener.converter.UserToUserDtoConverter;
import cc.hrva.urlshortener.dto.JWTTokenDto;
import cc.hrva.urlshortener.security.ClientIpResolver;
import cc.hrva.urlshortener.dto.LoginDto;
import cc.hrva.urlshortener.dto.UserDto;
import cc.hrva.urlshortener.dto.UserRegisterDto;
import cc.hrva.urlshortener.exception.NoAuthorizationException;
import cc.hrva.urlshortener.model.User;
import cc.hrva.urlshortener.service.impl.DefaultAuthService;
import cc.hrva.urlshortener.validator.UserValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultAuthServiceTest {

    private AuthService authService;

    @Mock
    private UserService userService;

    @Mock
    private UserValidator userValidator;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private LoginAttemptService loginAttemptService;

    @Mock
    private UserToUserDtoConverter userToUserDtoConverter;

    @Mock
    private AuthenticationManagerBuilder authenticationManagerBuilder;

    @Mock
    private UserRegisterDtoToUserConverter userRegisterDtoToUserConverter;

    @Mock
    private ClientIpResolver clientIpResolver;

    @Mock
    private AppProperties appProperties;

    @Mock
    private HttpServletRequest request;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        this.authService = new DefaultAuthService(userService, userValidator, tokenProvider, loginAttemptService, clientIpResolver, userToUserDtoConverter, authenticationManagerBuilder, userRegisterDtoToUserConverter, appProperties);
    }

    @Test
    void shouldRegisterUser() {
        final var registerDto = UserRegisterDto.builder().email("test@example.com").password("password123").build();
        final var user = User.builder().email("test@example.com").build();

        when(userRegisterDtoToUserConverter.convert(registerDto)).thenReturn(user);
        when(userService.register(user)).thenReturn(user);

        final var result = authService.register(registerDto);

        assertThat(result).isEqualTo("test@example.com");
        verify(userValidator).checkRegistrationEnabled();
        verify(userValidator).checkEmailUniqueness("test@example.com");
    }

    @Test
    void shouldLoginSuccessfully() {
        final var loginDto = LoginDto.builder().email("test@example.com").password("password123").build();
        final var user = User.builder().id(1L).email("test@example.com").build();
        final var userDto = UserDto.builder().id(1L).email("test@example.com").build();

        when(clientIpResolver.getClientIp(request)).thenReturn("127.0.0.1");
        when(loginAttemptService.isBlocked("127.0.0.1")).thenReturn(false);
        when(authenticationManagerBuilder.getObject()).thenReturn(authenticationManager);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(appProperties.getJwtTokenValiditySeconds()).thenReturn(36000L);
        when(tokenProvider.createToken(authentication, 36000L)).thenReturn("jwtToken");
        when(userService.fetchUserFromEmail("test@example.com")).thenReturn(user);
        when(userToUserDtoConverter.convert(user)).thenReturn(userDto);

        final var response = authService.login(loginDto, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isEqualTo("Bearer jwtToken");
        assertThat(response.getBody().getUser()).isEqualTo(userDto);
        assertThat(response.getHeaders().getFirst(JwtFilter.AUTHORIZATION_HEADER)).isEqualTo("Bearer jwtToken");
        verify(userService).userHasLoggedIn(user);
    }

    @Test
    void shouldThrowWhenBlocked() {
        final var loginDto = LoginDto.builder().email("test@example.com").password("password123").build();

        when(clientIpResolver.getClientIp(request)).thenReturn("127.0.0.1");
        when(loginAttemptService.isBlocked("127.0.0.1")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(loginDto, request))
                .isInstanceOf(NoAuthorizationException.class)
                .hasMessage("Request has been blocked");
    }

}
