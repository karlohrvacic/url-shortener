package cc.hrva.urlshortener.service.impl;

import cc.hrva.urlshortener.configuration.properties.AppProperties;
import cc.hrva.urlshortener.converter.UserToUserDtoConverter;
import cc.hrva.urlshortener.converter.UserUpdateDtoToUserConverter;
import cc.hrva.urlshortener.dto.PasswordResetDto;
import cc.hrva.urlshortener.dto.RequestPasswordResetDto;
import cc.hrva.urlshortener.dto.UpdatePasswordDto;
import cc.hrva.urlshortener.dto.UserDto;
import cc.hrva.urlshortener.dto.UserSearchDto;
import cc.hrva.urlshortener.dto.UserUpdateDto;
import cc.hrva.urlshortener.exception.ApiException;
import cc.hrva.urlshortener.exception.NoAuthorizationException;
import cc.hrva.urlshortener.exception.UserNotFoundException;
import cc.hrva.urlshortener.model.User;
import cc.hrva.urlshortener.repository.UserRepository;
import cc.hrva.urlshortener.repository.specification.UserSpecification;
import cc.hrva.urlshortener.service.ResetTokenService;
import cc.hrva.urlshortener.service.UserService;
import cc.hrva.urlshortener.validator.AuthValidator;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DefaultUserService implements UserService {

    private final AppProperties appProperties;
    private final AuthValidator authValidator;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ResetTokenService resetTokenService;
    private final UserToUserDtoConverter userToUserDtoConverter;
    private final DefaultSendingEmailService sendingEmailService;
    private final UserUpdateDtoToUserConverter userUpdateDtoToUserConverter;

    @Override
    @Transactional
    public User register(final User user) {
        final var savedUser = userRepository.save(user);
        log.info("User registered email={}", savedUser.getEmail());
        sendingEmailService.sendWelcomeEmail(savedUser);

        return savedUser;
    }

    @Override
    public User getUserFromToken() {
        final var username = SecurityContextHolder.getContext().getAuthentication().getName();

        return userRepository.findByEmail(username).orElse(null);
    }

    @Override
    public UserDto fetchCurrentUser() {
        return Optional.ofNullable(getUserFromToken())
                .map(userToUserDtoConverter::convert)
                .orElseThrow(() -> new NoAuthorizationException("Invalid credentials"));
    }

    @Override
    public User fetchUserFromEmail(final String email) {
        log.info("User login email={}", email);

        final var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (Boolean.FALSE.equals(user.getActive())) {
            throw new UserNotFoundException("User is inactive, please contact administrator");
        }

        return user;
    }

    @Override
    @Transactional
    public void persistUser(final User user) {
        userRepository.save(user);
    }

    @Override
    public Page<User> fetchAllUsers(final Pageable pageable, final UserSearchDto search) {
        final var spec = buildSpecification(search);

        if (spec == null) {
            return userRepository.findAll(pageable);
        }
        return userRepository.findAll(spec, pageable);
    }

    private Specification<User> buildSpecification(final UserSearchDto search) {
        Specification<User> spec = null;

        if (StringUtils.isNotEmpty(search.getSearch())) {
            spec = UserSpecification.search(search.getSearch());
        }
        if (search.getActive() != null) {
            spec = (spec == null) ? UserSpecification.hasActive(search.getActive()) : spec.and(UserSpecification.hasActive(search.getActive()));
        }

        return spec;
    }

    @Override
    @Transactional
    public void deleteUserById(final Long id) {
        log.info("Delete user id={}", id);
        userRepository.deleteById(id);
    }

    @Override
    @Transactional
    public User updateUser(final UserUpdateDto userUpdateDto) {
        log.info("Update user id={}", userUpdateDto.getId());

        final var existingUser = userRepository.findById(userUpdateDto.getId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (existingUser.getAuthProvider() != null && !"local".equals(existingUser.getAuthProvider())
                && userUpdateDto.getEmail() != null && !userUpdateDto.getEmail().equals(existingUser.getEmail())) {
            throw new ApiException("Email cannot be changed for %s accounts".formatted(existingUser.getAuthProvider()));
        }

        return userRepository.save(Objects.requireNonNull(userUpdateDtoToUserConverter.convert(userUpdateDto)));
    }

    @Override
    @Transactional
    public User updatePassword(final UpdatePasswordDto updatePasswordDto) {
        final var user = getUserFromToken();
        authValidator.passwordMatchesCurrentPassword(user, updatePasswordDto.getOldPassword());
        user.setPassword(passwordEncoder.encode(updatePasswordDto.getNewPassword()));
        log.info("Password changed for user id={}", user.getId());

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void sendPasswordResetLinkToUser(final RequestPasswordResetDto requestPasswordResetDto) {
        log.info("Password reset requested email={}", requestPasswordResetDto.getEmail());

        final var user = userRepository.findByEmail(requestPasswordResetDto.getEmail());

        if (user.isPresent()) {
            resetTokenService.deactivateActiveResetTokenIfExists(user.get());
            sendingEmailService.sendEmailForgotPassword(user.get(), resetTokenService.createTokenForUser(user.get()));
        }
    }

    @Override
    @Transactional
    public User resetPassword(final PasswordResetDto passwordResetDto) {
        log.info("Password reset for user email={}", passwordResetDto.getEmail());

        final var user = userRepository.findByEmail(passwordResetDto.getEmail())
                .orElseThrow(() -> new NoAuthorizationException("Invalid credentials"));

        final var token = resetTokenService.getResetTokenFromUserAndToken(user, passwordResetDto.getToken());

        if (token.isActive()) {
            user.setPassword(passwordEncoder.encode(passwordResetDto.getPassword()));
            resetTokenService.deactivateAndSaveToken(token);

            return userRepository.save(user);
        }

        throw new NoAuthorizationException("Token expired");
    }

    @Override
    @Transactional
    public void deactivateUnusedUserAccounts() {
        final var users = userRepository.findByLastLoginIsLessThanEqualAndActiveTrue(LocalDateTime.now()
                .minusDays(appProperties.getDeactivateUserAccountAfterDays())).stream()
                .map(this::deactivateUser)
                .toList();

        userRepository.saveAll(users);
        if (!users.isEmpty()) {
            log.info("Deactivated {} users", users.size());
        }
    }

    @Override
    @Transactional
    public void userHasLoggedIn(final User user) {
        user.userLoggedIn();
        persistUser(user);
    }

    private User deactivateUser(final User user) {
        user.setActive(false);
        log.info("Deactivated user with id {}", user.getId());

        return user;
    }

}
