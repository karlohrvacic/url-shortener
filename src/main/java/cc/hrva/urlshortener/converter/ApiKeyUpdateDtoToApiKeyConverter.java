package cc.hrva.urlshortener.converter;

import cc.hrva.urlshortener.exception.ApiKeyNotFoundException;
import cc.hrva.urlshortener.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import cc.hrva.urlshortener.dto.ApiKeyUpdateDto;
import cc.hrva.urlshortener.model.ApiKey;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApiKeyUpdateDtoToApiKeyConverter implements Converter<ApiKeyUpdateDto, ApiKey> {

    private final ApiKeyRepository apiKeyRepository;

    @Override
    public ApiKey convert(final ApiKeyUpdateDto apiKeyUpdateDto) {
        final var existingApiKey = apiKeyRepository.findById(apiKeyUpdateDto.getId())
                .orElseThrow(() -> new ApiKeyNotFoundException(String.format("API key with ID %d has not been found",
                        apiKeyUpdateDto.getId())));
        existingApiKey.setApiCallsLimit(apiKeyUpdateDto.getApiCallsLimit());
        existingApiKey.setApiCallsUsed(apiKeyUpdateDto.getApiCallsUsed());
        existingApiKey.setExpirationDate(apiKeyUpdateDto.getExpirationDate());
        existingApiKey.setActive(apiKeyUpdateDto.getActive());

        return existingApiKey;
    }

}
