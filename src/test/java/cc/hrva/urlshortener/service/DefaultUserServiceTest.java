package cc.hrva.urlshortener.service;

import java.util.Collections;
import java.util.Optional;
import cc.hrva.urlshortener.configuration.properties.AppProperties;
import cc.hrva.urlshortener.converter.UserToUserDtoConverter;
import cc.hrva.urlshortener.converter.UserUpdateDtoToUserConverter;
import cc.hrva.urlshortener.dto.PasswordResetDto;
import cc.hrva.urlshortener.dto.RequestPasswordResetDto;
import cc.hrva.urlshortener.dto.UpdatePasswordDto;
import cc.hrva.urlshortener.dto.UserDto;
import cc.hrva.urlshortener.dto.UserUpdateDto;
import cc.hrva.urlshortener.exception.NoAuthorizationException;
import cc.hrva.urlshortener.exception.UserNotFoundException;
import cc.hrva.urlshortener.model.ResetToken;
import cc.hrva.urlshortener.model.User;
import cc.hrva.urlshortener.repository.UserRepository;
import cc.hrva.urlshortener.service.impl.DefaultSendingEmailService;
import cc.hrva.urlshortener.service.impl.DefaultUserService;
import cc.hrva.urlshortener.validator.AuthValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class DefaultUserServiceTest {

    private UserService userService;

    @Mock
    private AppProperties appProperties;

    @Mock
    private AuthValidator authValidator;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ResetTokenService resetTokenService;

    @Mock
    private UserToUserDtoConverter userToUserDtoConverter;

    @Mock
    private DefaultSendingEmailService sendingEmailService;

    @Mock
    private UserUpdateDtoToUserConverter userUpdateDtoToUserConverter;

    @BeforeEach
    void setUp() {
        this.userService = new DefaultUserService(appProperties, authValidator, userRepository, passwordEncoder, resetTokenService, userToUserDtoConverter, sendingEmailService, userUpdateDtoToUserConverter);
        lenient().when(userRepository.findByEmail(null)).thenReturn(Optional.empty());
    }

    @Test
    void shouldRegisterUser() {
        final var user = User.builder().email("test@example.com").build();
        when(userRepository.save(user)).thenReturn(user);

        final var result = userService.register(user);

        assertThat(result.getEmail()).isEqualTo("test@example.com");
        verify(sendingEmailService).sendWelcomeEmail(user);
    }

    @Test
    void shouldFetchCurrentUser() {
        final var user = User.builder().id(1L).email("test@example.com").build();
        final var userDto = UserDto.builder().id(1L).email("test@example.com").build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("test@example.com", null)
        );
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(userToUserDtoConverter.convert(user)).thenReturn(userDto);

        final var result = userService.fetchCurrentUser();

        assertThat(result.getEmail()).isEqualTo("test@example.com");
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldFailFetchCurrentUserWhenNotAuthenticated() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(null, null)
        );
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.fetchCurrentUser())
                .isInstanceOf(NoAuthorizationException.class)
                .hasMessage("Invalid credentials");
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldFetchUserFromEmail() {
        final var user = User.builder().id(1L).email("test@example.com").active(true).build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        final var result = userService.fetchUserFromEmail("test@example.com");

        assertThat(result).isEqualTo(user);
    }

    @Test
    void shouldFailFetchUserFromEmailWhenInactive() {
        final var user = User.builder().id(1L).email("test@example.com").active(false).build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.fetchUserFromEmail("test@example.com"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User is inactive, please contact administrator");
    }

    @Test
    void shouldFetchAllUsers() {
        final var pageable = PageRequest.of(0, 20);
        final var users = new PageImpl<>(Collections.singletonList(User.builder().id(1L).build()));
        when(userRepository.findAll(pageable)).thenReturn(users);

        assertThat(userService.fetchAllUsers(pageable)).isEqualTo(users);
    }

    @Test
    void shouldDeleteUserById() {
        userService.deleteUserById(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    void shouldUpdateUser() {
        final var updateDto = UserUpdateDto.builder().id(1L).build();
        final var existingUser = User.builder().id(1L).authProvider("local").build();
        final var user = User.builder().id(1L).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userUpdateDtoToUserConverter.convert(updateDto)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);

        final var result = userService.updateUser(updateDto);

        assertThat(result).isEqualTo(user);
    }

    @Test
    void shouldUpdatePassword() {
        final var user = User.builder().id(1L).password("oldEncoded").build();
        final var updateDto = UpdatePasswordDto.builder().oldPassword("oldPassword").newPassword("newPassword").build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("test@example.com", null)
        );
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPassword")).thenReturn("newEncoded");
        when(userRepository.save(user)).thenReturn(user);

        final var result = userService.updatePassword(updateDto);

        assertThat(result.getPassword()).isEqualTo("newEncoded");
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldSendPasswordResetLink() {
        final var requestDto = RequestPasswordResetDto.builder().email("test@example.com").build();
        final var user = User.builder().id(1L).email("test@example.com").build();
        final var token = ResetToken.builder().id(1L).build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(resetTokenService.createTokenForUser(user)).thenReturn(token);

        userService.sendPasswordResetLinkToUser(requestDto);

        verify(resetTokenService).deactivateActiveResetTokenIfExists(user);
        verify(sendingEmailService).sendEmailForgotPassword(user, token);
    }

    @Test
    void shouldNotSendPasswordResetLinkWhenUserNotFound() {
        final var requestDto = RequestPasswordResetDto.builder().email("test@example.com").build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        userService.sendPasswordResetLinkToUser(requestDto);

        verify(resetTokenService, org.mockito.Mockito.never()).deactivateActiveResetTokenIfExists(any());
    }

    @Test
    void shouldResetPassword() {
        final var resetDto = PasswordResetDto.builder().email("test@example.com").token("token").password("newPassword").build();
        final var user = User.builder().id(1L).email("test@example.com").build();
        final var token = ResetToken.builder().id(1L).active(true).build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(resetTokenService.getResetTokenFromUserAndToken(user, "token")).thenReturn(token);
        when(passwordEncoder.encode("newPassword")).thenReturn("newEncoded");
        when(userRepository.save(user)).thenReturn(user);
        org.mockito.Mockito.doAnswer(inv -> {
            ResetToken t = inv.getArgument(0);
            t.setActive(false);
            return null;
        }).when(resetTokenService).deactivateAndSaveToken(token);

        final var result = userService.resetPassword(resetDto);

        assertThat(result.getPassword()).isEqualTo("newEncoded");
        assertThat(token.isActive()).isFalse();
    }

    @Test
    void shouldFailResetPasswordWhenTokenInactive() {
        final var resetDto = PasswordResetDto.builder().email("test@example.com").token("token").password("newPassword").build();
        final var user = User.builder().id(1L).email("test@example.com").build();
        final var token = ResetToken.builder().id(1L).active(false).build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(resetTokenService.getResetTokenFromUserAndToken(user, "token")).thenReturn(token);

        assertThatThrownBy(() -> userService.resetPassword(resetDto))
                .isInstanceOf(NoAuthorizationException.class)
                .hasMessage("Token expired");
    }

    @Test
    void shouldDeactivateUnusedUserAccounts() {
        final var user = User.builder().id(1L).active(true).build();
        when(appProperties.getDeactivateUserAccountAfterDays()).thenReturn(30L);
        when(userRepository.findByLastLoginIsLessThanEqualAndActiveTrue(any(java.time.LocalDateTime.class)))
                .thenReturn(Collections.singletonList(user));
        when(userRepository.saveAll(any())).thenReturn(Collections.singletonList(user));

        userService.deactivateUnusedUserAccounts();

        assertThat(user.getActive()).isFalse();
    }

    @Test
    void shouldUserHasLoggedIn() {
        final var user = User.builder().id(1L).build();
        when(userRepository.save(user)).thenReturn(user);

        userService.userHasLoggedIn(user);

        assertThat(user.getLastLogin()).isNotNull();
    }
}
