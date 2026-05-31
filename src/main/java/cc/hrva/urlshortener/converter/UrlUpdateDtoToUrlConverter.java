package cc.hrva.urlshortener.converter;

import cc.hrva.urlshortener.exception.UrlNotFoundException;
import cc.hrva.urlshortener.repository.UrlRepository;
import cc.hrva.urlshortener.util.TagNormalizer;
import lombok.RequiredArgsConstructor;
import cc.hrva.urlshortener.dto.UrlUpdateDto;
import cc.hrva.urlshortener.model.Url;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UrlUpdateDtoToUrlConverter implements Converter<UrlUpdateDto, Url> {

    private final UrlRepository urlRepository;

    @Override
    public Url convert(final UrlUpdateDto urlUpdateDto) {
        final var existingUrl = urlRepository.findById(urlUpdateDto.getId())
                .orElseThrow(() -> new UrlNotFoundException(String.format("Url with id %d doesn't exist", urlUpdateDto.getId())));

        if (urlUpdateDto.getVisitLimit() == null || urlUpdateDto.getVisitLimit() <= 0) {
            existingUrl.setVisitLimit(null);
        } else {
            existingUrl.setVisitLimit(urlUpdateDto.getVisitLimit());
        }

        if (urlUpdateDto.getExpirationDate() == null) {
            existingUrl.setExpirationDate(null);
        } else {
            existingUrl.setExpirationDate(urlUpdateDto.getExpirationDate());
        }

        if (urlUpdateDto.getTags() != null) {
            existingUrl.setTags(TagNormalizer.normalize(urlUpdateDto.getTags()));
        }
        return existingUrl;
    }

}
