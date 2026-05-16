package cc.hrva.urlshortener.converter;

import java.util.Optional;
import cc.hrva.urlshortener.dto.ApiKeyUpdateDto;
import cc.hrva.urlshortener.exception.ApiKeyNotFoundException;
import cc.hrva.urlshortener.model.ApiKey;
import cc.hrva.urlshortener.repository.ApiKeyRepository;
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
class ApiKeyUpdateDtoToApiKeyConverterTest {

    private ApiKeyUpdateDtoToApiKeyConverter converter;

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @BeforeEach
    void setUp() {
        this.converter = new ApiKeyUpdateDtoToApiKeyConverter(this.apiKeyRepository);
    }

    @Test
    void shouldConvert() {
        final ApiKeyUpdateDto apiKeyUpdateDto = ApiKeyUpdateDto.builder().id(1L).active(true).build();
        final ApiKey apiKey = ApiKey.builder().id(1L).active(true).build();

        when(apiKeyRepository.findById(anyLong())).thenReturn(Optional.ofNullable(apiKey));

        assertThat(converter.convert(apiKeyUpdateDto)).isEqualTo(apiKey);
    }

    @Test
    void shouldFailConvert() {
        final ApiKeyUpdateDto apiKeyUpdateDto = ApiKeyUpdateDto.builder().id(1L).build();

        when(apiKeyRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatCode(() -> converter.convert(apiKeyUpdateDto))
                .isInstanceOf(ApiKeyNotFoundException.class)
                .hasMessage(String.format("API key with ID %d has not been found", apiKeyUpdateDto.getId()));
    }
}