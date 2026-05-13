package cc.hrva.urlshortener.converter;

import java.util.List;
import cc.hrva.urlshortener.configuration.properties.AppProperties;
import cc.hrva.urlshortener.dto.UserRegisterDto;
import cc.hrva.urlshortener.model.User;
import cc.hrva.urlshortener.model.codebook.Authorities;
import cc.hrva.urlshortener.service.AuthoritiesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRegisterDtoToUserConverterTest {

    private UserRegisterDtoToUserConverter converter;

    @Mock
    private AppProperties appProperties;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthoritiesService authoritiesService;

    @BeforeEach
    void setUp() {
        this.converter = new UserRegisterDtoToUserConverter(appProperties, passwordEncoder, authoritiesService);
    }

    @Test
    void shouldConvertUserRegisterDtoToUser() {
        final var authority = Authorities.builder().id(1L).name("ROLE_USER").build();
        final var registerDto = UserRegisterDto.builder()
                .name("Test User")
                .email("test@example.com")
                .password("password123")
                .build();

        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(authoritiesService.getDefaultAuthority()).thenReturn(authority);
        when(appProperties.getUserApiKeySlots()).thenReturn(3L);

        final var user = converter.convert(registerDto);

        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getPassword()).isEqualTo("encodedPassword");
        assertThat(user.getAuthorities()).containsExactly(authority);
        assertThat(user.getApiKeySlots()).isEqualTo(3L);
    }
}
