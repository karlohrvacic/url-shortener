package cc.hrva.urlshortener.converter;

import java.util.Optional;
import cc.hrva.urlshortener.dto.UrlUpdateDto;
import cc.hrva.urlshortener.exception.UrlNotFoundException;
import cc.hrva.urlshortener.model.Url;
import cc.hrva.urlshortener.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UrlUpdateDtoToUrlConverterTest {

    private UrlUpdateDtoToUrlConverter converter;

    @Mock
    private UrlRepository urlRepository;

    @BeforeEach
    void setUp() {
        this.converter = new UrlUpdateDtoToUrlConverter(this.urlRepository);
    }

    @Test
    void shouldUpdateVisitLimitAndExpirationDate() {
        final var existingUrl = Url.builder().id(1L).longUrl("https://example.com").build();
        final var updateDto = UrlUpdateDto.builder().id(1L).visitLimit(100L).expirationDate(java.time.LocalDateTime.now().plusDays(1)).build();

        when(urlRepository.findById(anyLong())).thenReturn(Optional.of(existingUrl));

        final var result = converter.convert(updateDto);

        assertThat(result.getVisitLimit()).isEqualTo(100L);
        assertThat(result.getExpirationDate()).isEqualTo(updateDto.getExpirationDate());
    }

    @Test
    void shouldClearVisitLimitWhenNullOrZero() {
        final var existingUrl = Url.builder().id(1L).longUrl("https://example.com").visitLimit(50L).build();
        final var updateDto = UrlUpdateDto.builder().id(1L).visitLimit(null).build();

        when(urlRepository.findById(anyLong())).thenReturn(Optional.of(existingUrl));

        final var result = converter.convert(updateDto);

        assertThat(result.getVisitLimit()).isNull();
    }

    @Test
    void shouldClearVisitLimitWhenNegativeOrZero() {
        final var existingUrl = Url.builder().id(1L).longUrl("https://example.com").visitLimit(50L).build();
        final var updateDto = UrlUpdateDto.builder().id(1L).visitLimit(0L).build();

        when(urlRepository.findById(anyLong())).thenReturn(Optional.of(existingUrl));

        final var result = converter.convert(updateDto);

        assertThat(result.getVisitLimit()).isNull();
    }

    @Test
    void shouldClearExpirationDateWhenNull() {
        final var existingUrl = Url.builder().id(1L).longUrl("https://example.com").expirationDate(java.time.LocalDateTime.now()).build();
        final var updateDto = UrlUpdateDto.builder().id(1L).expirationDate(null).build();

        when(urlRepository.findById(anyLong())).thenReturn(Optional.of(existingUrl));

        final var result = converter.convert(updateDto);

        assertThat(result.getExpirationDate()).isNull();
    }

    @Test
    void shouldFailWhenUrlNotFound() {
        final var updateDto = UrlUpdateDto.builder().id(1L).build();

        when(urlRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> converter.convert(updateDto))
                .isInstanceOf(UrlNotFoundException.class)
                .hasMessage("Url with id 1 doesn't exist");
    }
}
