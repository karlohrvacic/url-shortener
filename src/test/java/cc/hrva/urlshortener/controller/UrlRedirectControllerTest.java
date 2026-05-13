package cc.hrva.urlshortener.controller;

import cc.hrva.urlshortener.service.AuthService;
import cc.hrva.urlshortener.service.UrlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.RedirectView;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UrlRedirectControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UrlService urlService;

    @Mock
    private AuthService authService;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(new UrlRedirectController(urlService, authService, null)).build();
    }

    @Test
    void shouldRedirectToLongUrl() throws Exception {
        final var redirectView = new RedirectView("https://example.com");
        when(authService.getClientIP(any())).thenReturn("127.0.0.1");
        when(urlService.redirectResultUrl("short", "127.0.0.1")).thenReturn(redirectView);

        mockMvc.perform(get("/short"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://example.com"));
    }

    @Test
    void shouldRedirectRootToFrontend() throws Exception {
        final var redirectView = new RedirectView("https://frontend.com");
        when(urlService.redirectResultUrl(null, null)).thenReturn(redirectView);

        mockMvc.perform(get("/"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://frontend.com"));
    }
}
