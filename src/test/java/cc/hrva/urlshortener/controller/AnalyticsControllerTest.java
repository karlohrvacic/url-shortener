package cc.hrva.urlshortener.controller;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import cc.hrva.urlshortener.dto.AnalyticsOverviewDto;
import cc.hrva.urlshortener.dto.AnalyticsOverviewDto.TopUrl;
import cc.hrva.urlshortener.dto.UrlAnalyticsDto;
import cc.hrva.urlshortener.dto.UrlAnalyticsDto.DailyCount;
import cc.hrva.urlshortener.service.AnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(new AnalyticsController(analyticsService)).build();
    }

    @Test
    void shouldReturnOverview() throws Exception {
        final var overview = new AnalyticsOverviewDto(2, 1, 1, 60,
                List.of(new TopUrl(1L, "a", "https://a.com", 50)));
        when(analyticsService.getOverview()).thenReturn(overview);

        mockMvc.perform(get("/api/v1/analytics/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVisits").value(60))
                .andExpect(jsonPath("$.topUrls[0].visits").value(50));
    }

    @Test
    void shouldReturnUrlAnalytics() throws Exception {
        final var analytics = new UrlAnalyticsDto(7L, "a", "https://a.com", "ACTIVE", 3, 2, 7, null, null,
                List.of(new DailyCount(LocalDate.now(), 1)));
        when(analyticsService.getUrlAnalytics(7L)).thenReturn(analytics);

        mockMvc.perform(get("/api/v1/analytics/urls/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uniqueRecentVisitors").value(2))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}
