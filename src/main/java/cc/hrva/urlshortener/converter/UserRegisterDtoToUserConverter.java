package cc.hrva.urlshortener.converter;

import cc.hrva.urlshortener.configuration.properties.AppProperties;
import java.util.List;
import lombok.RequiredArgsConstructor;
import cc.hrva.urlshortener.dto.UserRegisterDto;
import cc.hrva.urlshortener.model.User;
import cc.hrva.urlshortener.service.AuthoritiesService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserRegisterDtoToUserConverter implements Converter<UserRegisterDto, User> {

    private final AppProperties appProperties;
    private final PasswordEncoder passwordEncoder;
    private final AuthoritiesService authoritiesService;

    @Override
    public User convert(final UserRegisterDto userRegisterDto) {
        return User.builder()
                .email(userRegisterDto.getEmail())
                .password(passwordEncoder.encode(userRegisterDto.getPassword()))
                .authorities(List.of(authoritiesService.getDefaultAuthority()))
                .apiKeySlots(appProperties.getUserApiKeySlots())
                .emailVerified(false)
                .build();
    }

}
