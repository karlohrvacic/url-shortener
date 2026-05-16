package cc.hrva.urlshortener.converter;

import java.util.Optional;
import cc.hrva.urlshortener.dto.UserUpdateDto;
import cc.hrva.urlshortener.exception.UserNotFoundException;
import cc.hrva.urlshortener.model.User;
import cc.hrva.urlshortener.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserUpdateDtoToUserConverterTest {

    private UserUpdateDtoToUserConverter converter;

    @Mock
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        this.converter = new UserUpdateDtoToUserConverter(this.userRepository);
    }

    @Test
    void shouldConvert() {
        final UserUpdateDto userUpdateDto = UserUpdateDto.builder().id(1L).build();
        final User user = User.builder().id(1L).build();

        when(userRepository.findById(anyLong())).thenReturn(Optional.ofNullable(user));

        assertThat(converter.convert(userUpdateDto)).isEqualTo(user);
    }

    @Test
    void shouldFailConvert() {
        final UserUpdateDto userUpdateDto = UserUpdateDto.builder().id(1L).build();

        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatCode(() -> converter.convert(userUpdateDto))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage(String.format("User with ID %d has not been found", userUpdateDto.getId()));
    }
}