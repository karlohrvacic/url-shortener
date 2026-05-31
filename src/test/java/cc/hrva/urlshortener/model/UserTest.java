package cc.hrva.urlshortener.model;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void shouldSetLastLoginOnUserLoggedIn() {
        final var user = new User();
        user.userLoggedIn();

        assertThat(user.getLastLogin()).isNotNull();
        assertThat(user.getLastLogin()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void shouldDefaultToActiveOnCreateWhenUnset() {
        final var user = new User();
        user.onCreate();

        assertThat(user.getCreateDate()).isNotNull();
        assertThat(user.getActive()).isTrue();
    }

    @Test
    void shouldPreserveExplicitActiveFlagOnCreate() {
        final var user = User.builder().active(false).build();
        user.onCreate();

        assertThat(user.getActive()).isFalse();
    }
}
