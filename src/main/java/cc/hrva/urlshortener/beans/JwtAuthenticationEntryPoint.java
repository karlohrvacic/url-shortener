package cc.hrva.urlshortener.beans;

import cc.hrva.urlshortener.exception.ApiException;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse, final AuthenticationException e) throws IOException {
        final var apiError = new ApiException(UNAUTHORIZED);
        apiError.setMessage(e.getMessage());
        apiError.setDebugMessage(e.getMessage());

        httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        httpServletResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        httpServletResponse.getWriter().write(objectMapper.writeValueAsString(apiError));
    }

}
