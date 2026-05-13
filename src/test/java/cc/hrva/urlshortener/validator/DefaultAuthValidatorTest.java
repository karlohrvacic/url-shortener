package cc.hrva.urlshortener.validator;

import cc.hrva.urlshortener.exception.NoAuthorizationException;
import cc.hrva.urlshortener.model.User;
import cc.hrva.urlshortener.validator.impl.DefaultAuthValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultAuthValidatorTest {

    private AuthValidator authValidator;

    @Mock
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        this.authValidator = new DefaultAuthValidator(passwordEncoder);
    }

    @Test
    void shouldValidatePasswordMatches() {
        final var user = User.builder().password("encodedPassword").build();

        when(passwordEncoder.matches("rawPassword", "encodedPassword")).thenReturn(true);

        assertThatCode(() -> authValidator.passwordMatchesCurrentPassword(user, "rawPassword"))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldFailWhenPasswordDoesNotMatch() {
        final var user = User.builder().password("encodedPassword").build();

        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        assertThatThrownBy(() -> authValidator.passwordMatchesCurrentPassword(user, "wrongPassword"))
                .isInstanceOf(NoAuthorizationException.class)
                .hasMessage("Password doesn't match existing password");
    }
}
