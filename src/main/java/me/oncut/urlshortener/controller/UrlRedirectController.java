package me.oncut.urlshortener.controller;

import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import me.oncut.urlshortener.service.UrlService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@CommonsLog
@RestController
@RequiredArgsConstructor
public class UrlRedirectController {

    private final HttpServletRequest request;
    private final UrlService urlService;

    @GetMapping({"/{short}"})
    public RedirectView fetchUrlByShortUrlOrRedirectToFrontend(@PathVariable("short") final String shortUrl) {
        String remoteAddress = "";
        if (request != null) {
            remoteAddress = request.getHeader("X-FORWARDED-FOR");
            if (StringUtils.isEmpty(remoteAddress) || "".equals(remoteAddress)) {
                remoteAddress = request.getRemoteAddr();
            }
        }

        return urlService.redirectResultUrl(shortUrl, remoteAddress);
    }

    @GetMapping({"/"})
    public RedirectView rootRedirectToFrontend() {
        return urlService.redirectResultUrl(null, null);
    }
}
