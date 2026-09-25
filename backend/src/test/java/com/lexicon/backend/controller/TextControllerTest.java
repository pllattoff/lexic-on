package com.lexicon.backend.controller;

import tools.jackson.databind.ObjectMapper;
import com.lexicon.backend.dto.ProcessTextRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TextControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void process_returnsBadRequest_whenTextIsBlank() throws Exception {
        // GIVEN
        String requestBody = objectMapper.writeValueAsString(new ProcessTextRequest(" "));

        // WHEN
        mockMvc.perform(post("/api/text/process")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
        // THEN
                .andExpect(status().isBadRequest());
    }

    @Test
    void process_returnsForbidden_whenCsrfTokenIsMissing() throws Exception {
        // GIVEN
        String requestBody = objectMapper.writeValueAsString(new ProcessTextRequest("Hello world"));

        // WHEN posting it without a CSRF token
        mockMvc.perform(post("/api/text/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
        // THEN the response is 403 Forbidden
                .andExpect(status().isForbidden());
    }

    @Test
    void process_returnsProcessedText_whenCalledByGuest() throws Exception {
        // GIVEN
        String requestBody = objectMapper.writeValueAsString(new ProcessTextRequest("Hello world"));

        // WHEN posting it with a valid CSRF token and no authentication
        mockMvc.perform(post("/api/text/process")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
        // THEN the response is 200 OK with the word tokens, their positions, and lemmas
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Hello world"))
                .andExpect(jsonPath("$.tokens", hasSize(2)))
                .andExpect(jsonPath("$.tokens[0].start").value(0))
                .andExpect(jsonPath("$.tokens[0].end").value(5))
                .andExpect(jsonPath("$.tokens[0].lemma").value("hello"))
                .andExpect(jsonPath("$.tokens[1].start").value(6))
                .andExpect(jsonPath("$.tokens[1].end").value(11))
                .andExpect(jsonPath("$.tokens[1].lemma").value("world"));
    }
}
