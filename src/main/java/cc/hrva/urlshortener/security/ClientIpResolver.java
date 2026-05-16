package cc.hrva.urlshortener.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {

    public String getClientIp(final HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        return request.getRemoteAddr();
    }

}
