package com.cruz.url_shortener;

import com.cruz.url_shortener.config.BaseIntegrationTest;
import com.cruz.url_shortener.dto.RegistrationRequestDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest extends BaseIntegrationTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @Test
    void shouldRegisterUser() throws Exception{
        RegistrationRequestDto requestDto = new RegistrationRequestDto();
        requestDto.setUserName("myuser1");
        requestDto.setPassword("password12345!");
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userName").value("myuser1"));
    }
    @Test
    void shouldBlockDuplicateUsername() throws Exception{
        RegistrationRequestDto requestDto = new RegistrationRequestDto();
        requestDto.setUserName("myuser1");
        requestDto.setPassword("password12345!");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userName").value("myuser1"));
        RegistrationRequestDto requestDto2 = new RegistrationRequestDto();
        requestDto2.setUserName("myuser1");
        requestDto2.setPassword("password12345!");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto2)))
                .andExpect(status().isConflict());

    }
}


