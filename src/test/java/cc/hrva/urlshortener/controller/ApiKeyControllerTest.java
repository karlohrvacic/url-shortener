package cc.hrva.urlshortener.controller;

import java.util.Collections;
import cc.hrva.urlshortener.dto.ApiKeyUpdateDto;
import cc.hrva.urlshortener.model.ApiKey;
import cc.hrva.urlshortener.service.ApiKeyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ApiKeyControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ApiKeyService apiKeyService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(new ApiKeyController(apiKeyService)).build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void shouldGenerateNewApiKey() throws Exception {
        final var apiKey = ApiKey.builder().id(1L).key("new-key").build();
        when(apiKeyService.generateNewApiKey()).thenReturn(apiKey);

        mockMvc.perform(get("/api/v1/api-key/new"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("new-key"));
    }

    @Test
    void shouldFetchMyApiKeys() throws Exception {
        final var apiKeys = Collections.singletonList(ApiKey.builder().id(1L).build());
        when(apiKeyService.fetchMyApiKeys()).thenReturn(apiKeys);

        mockMvc.perform(get("/api/v1/api-key/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1));
    }

    @Test
    void shouldFetchAllApiKeys() throws Exception {
        final var apiKeys = Collections.singletonList(ApiKey.builder().id(1L).build());
        when(apiKeyService.fetchAllApiKeys()).thenReturn(apiKeys);

        mockMvc.perform(get("/api/v1/api-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1));
    }

    @Test
    void shouldUpdateApiKey() throws Exception {
        final var apiKey = ApiKey.builder().id(1L).key("updated-key").build();
        when(apiKeyService.updateKey(any(ApiKeyUpdateDto.class))).thenReturn(apiKey);

        mockMvc.perform(put("/api/v1/api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApiKeyUpdateDto.builder().id(1L).build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void shouldRevokeApiKey() throws Exception {
        final var apiKey = ApiKey.builder().id(1L).active(false).build();
        when(apiKeyService.revokeApiKey(1L)).thenReturn(apiKey);

        mockMvc.perform(get("/api/v1/api-key/revoke/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }
}
