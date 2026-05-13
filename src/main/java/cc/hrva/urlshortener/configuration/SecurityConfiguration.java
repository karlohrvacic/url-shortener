package cc.hrva.urlshortener.configuration;

import cc.hrva.urlshortener.beans.JwtFilter;
import cc.hrva.urlshortener.configuration.properties.AppProperties;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfiguration {

    private final JwtFilter jwtFilter;
    private final AppProperties appProperties;
    private final UserDetailsService userDetailsService;

    @Bean
    public AuthenticationProvider authenticationProvider() {
        final var authenticationProvider = new DaoAuthenticationProvider(userDetailsService);
        authenticationProvider.setPasswordEncoder(new BCryptPasswordEncoder());

        return authenticationProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests
                                .requestMatchers("/api/v1/auth/**", "/api/v1/url/**", "/**")
                                .permitAll()
                                .anyRequest()
                                .authenticated()
                ).exceptionHandling(exceptionHandling ->
                        exceptionHandling
                                .authenticationEntryPoint(
                                        (request, response, ex) -> response.sendError(
                                                HttpServletResponse.SC_UNAUTHORIZED,
                                                ex.getMessage()
                                        )
                                )
                ).sessionManagement(sessionManagement ->
                        sessionManagement
                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                ).authenticationProvider(authenticationProvider())
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    @Bean
    public AuthenticationManager authenticationManager(final AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        final var configuration = new CorsConfiguration();
        final var source = new UrlBasedCorsConfigurationSource();

        configuration.setAllowedOrigins(List.of(appProperties.getFrontendUrl()));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST"));
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

}
