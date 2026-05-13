package cc.hrva.urlshortener.controller;

import java.util.Collections;
import cc.hrva.urlshortener.dto.CreateUrlDto;
import cc.hrva.urlshortener.dto.UrlUpdateDto;
import cc.hrva.urlshortener.model.PeekUrl;
import cc.hrva.urlshortener.model.Url;
import cc.hrva.urlshortener.service.UrlService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UrlControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UrlService urlService;

    @Mock
    private HttpServletRequest request;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(new UrlController(urlService, request)).build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void shouldSaveUrl() throws Exception {
        final var url = Url.builder().id(1L).longUrl("https://example.com").shortUrl("ex").build();
        when(urlService.saveUrlRouting(any(CreateUrlDto.class))).thenReturn(url);

        mockMvc.perform(post("/api/v1/url/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CreateUrlDto.builder().longUrl("https://example.com").build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.longUrl").value("https://example.com"));
    }

    @Test
    void shouldSaveUrlWithApiKey() throws Exception {
        final var url = Url.builder().id(1L).longUrl("https://example.com").shortUrl("ex").build();
        when(urlService.saveUrlWithApiKey(any(CreateUrlDto.class), eq("apikey"))).thenReturn(url);

        mockMvc.perform(post("/api/v1/url/new/apikey")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CreateUrlDto.builder().longUrl("https://example.com").build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortUrl").value("ex"));
    }

    @Test
    void shouldFetchUrlByShort() throws Exception {
        final var url = Url.builder().id(1L).longUrl("https://example.com").build();
        when(request.getHeader("X-FORWARDED-FOR")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(urlService.checkIPUniquenessAndReturnUrl("short", "127.0.0.1")).thenReturn(url);

        mockMvc.perform(get("/api/v1/url/redirect/short"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void shouldPeekUrlByShortUrl() throws Exception {
        final var peekUrl = PeekUrl.builder().longUrl("https://example.com").shortUrl("ex").build();
        when(urlService.peekUrlByShortUrl("short")).thenReturn(peekUrl);

        mockMvc.perform(get("/api/v1/url/peek/short"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.longUrl").value("https://example.com"));
    }

    @Test
    void shouldUpdateUrl() throws Exception {
        final var url = Url.builder().id(1L).longUrl("https://example.com").build();
        when(urlService.updateUrl(any(UrlUpdateDto.class))).thenReturn(url);

        mockMvc.perform(put("/api/v1/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UrlUpdateDto.builder().id(1L).build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void shouldGetAllMyUrlsWithApiKey() throws Exception {
        final var urls = Collections.singletonList(Url.builder().id(1L).build());
        when(urlService.getAllMyUrls("apikey")).thenReturn(urls);

        mockMvc.perform(get("/api/v1/url/my/apikey"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1));
    }

    @Test
    void shouldGetAllMyUrls() throws Exception {
        final var urls = Collections.singletonList(Url.builder().id(1L).build());
        when(urlService.getAllMyUrls(null)).thenReturn(urls);

        mockMvc.perform(get("/api/v1/url/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1));
    }

    @Test
    void shouldGetAllUrls() throws Exception {
        final var urls = Collections.singletonList(Url.builder().id(1L).build());
        when(urlService.getAllUrls()).thenReturn(urls);

        mockMvc.perform(get("/api/v1/url/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1));
    }

    @Test
    void shouldRevokeUrl() throws Exception {
        final var url = Url.builder().id(1L).active(false).build();
        when(urlService.revokeUrl(1L)).thenReturn(url);

        mockMvc.perform(get("/api/v1/url/deactivate/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void shouldDeleteUrl() throws Exception {
        mockMvc.perform(get("/api/v1/url/delete/1"))
                .andExpect(status().isNoContent());

        verify(urlService).deleteUrl(1L);
    }
}
