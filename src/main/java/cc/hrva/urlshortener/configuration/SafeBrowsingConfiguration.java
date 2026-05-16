package cc.hrva.urlshortener.configuration;

import cc.hrva.urlshortener.configuration.properties.GoogleApiProperties;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.safebrowsing.v5.Safebrowsing;
import java.io.IOException;
import java.security.GeneralSecurityException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SafeBrowsingConfiguration {

    @Bean
    public Safebrowsing safebrowsing(final GoogleApiProperties googleApiProperties) throws GeneralSecurityException, IOException {
        final var httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        final var jsonFactory = GsonFactory.getDefaultInstance();

        return new Safebrowsing.Builder(httpTransport, jsonFactory, null)
                .setApplicationName(googleApiProperties.getClientId())
                .build();
    }

}
