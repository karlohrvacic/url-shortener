package cc.hrva.urlshortener.service;

import cc.hrva.urlshortener.dto.CreateUrlDto;
import cc.hrva.urlshortener.dto.UrlResponse;
import cc.hrva.urlshortener.dto.UrlSearchDto;
import cc.hrva.urlshortener.dto.UrlUpdateDto;
import cc.hrva.urlshortener.model.PeekUrl;
import cc.hrva.urlshortener.model.Url;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.servlet.view.RedirectView;

public interface UrlService {

    Page<UrlResponse> getAllUrls(Pageable pageable);
    UrlResponse revokeUrl(Long id);
    void deleteUrl(Long id);
    void deactivateExpiredUrls();
    UrlResponse updateUrl(UrlUpdateDto url);
    Url getUrlByLongUrl(String longUrl);
    String generateShortUrl(Long length);
    UrlResponse saveUrlRouting(CreateUrlDto url);
    Page<UrlResponse> getAllMyUrls(String apiKey, Pageable pageable, UrlSearchDto search);
    PeekUrl peekUrlByShortUrl(String shortUrl);
    UrlResponse saveUrlWithApiKey(CreateUrlDto createUrlDto, String apiKey);
    RedirectView redirectResultUrl(String shortUrl, String clientIP);
    UrlResponse checkIPUniquenessAndReturnUrl(String shortUrl, String clientIP);
    byte[] exportMyUrlsAsCsv(String apiKey);

}
