package cc.hrva.urlshortener;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@EnableAsync
@EnableCaching
@OpenAPIDefinition(info = @Info(
        title = "hrva.cc URL Shortener API",
        version = "1.4.2",
        description = "Shorten URLs, track visits, manage links, and integrate via API keys.",
        contact = @Contact(name = "hrva.cc", email = "url-shortener@hrva.cc")
))
@EnableWebSecurity
@SpringBootApplication
public class UrlShortenerApplication {

    public static void main(final String[] args) {
        SpringApplication.run(UrlShortenerApplication.class, args);
    }

}
