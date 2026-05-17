package cc.hrva.urlshortener.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class ChangelogControllerTest {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ChangelogController())
            .build();

    @Test
    void shouldReturnChangelog() throws Exception {
        mockMvc.perform(get("/api/changelog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.releases").isArray())
                .andExpect(jsonPath("$.releases[0].version").isString());
    }

}
