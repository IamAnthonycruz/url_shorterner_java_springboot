package com.cruz.url_shortener;


import com.cruz.url_shortener.config.BaseIntegrationTest;
import com.cruz.url_shortener.dto.LoginRequestDto;
import com.cruz.url_shortener.dto.RegistrationRequestDto;

import com.cruz.url_shortener.dto.UrlRequestDto;
import com.cruz.url_shortener.dto.UrlResponseDto;
import com.cruz.url_shortener.entity.Url;
import com.cruz.url_shortener.entity.User;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class UrlControllerTest extends BaseIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    protected Cookie getSessionCookie(String userName, String password) throws Exception {
        RegistrationRequestDto registration = new RegistrationRequestDto();
        registration.setUserName(userName);
        registration.setPassword(password);

        MvcResult registrationResult = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registration))).andExpect(status().isCreated())
                .andReturn();
        LoginRequestDto login = new LoginRequestDto();
        login.setUserName(registration.getUserName());
        login.setPassword(registration.getPassword());
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login))).andExpect(status().isOk())
                .andReturn();
        return loginResult.getResponse().getCookie("session-id");
    };
    @Test
    void shouldShortenUrlWhenAuthenticated() throws Exception {
        Cookie sessionCookie = getSessionCookie("testuser", "12344231111111");
        UrlRequestDto requestDto = new UrlRequestDto();
        requestDto.setLongUrl("http://www.google.com");
        mockMvc.perform(post("/api/v1/short-urls").cookie(sessionCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.longUrl").value("http://www.google.com"))
                .andExpect(jsonPath("$.shortUrl").exists());
    }
    @Test
    void shouldNotShortenUrlWhenNotAuthenticated()throws Exception{
        UrlRequestDto urlRequestDto = new UrlRequestDto();
        urlRequestDto.setLongUrl("http://www.google.com");
        mockMvc.perform(post("/api/v1/short-urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(urlRequestDto)))
                .andExpect(status().isUnauthorized());
    }
    @Test
    void shouldRedirect()throws Exception{
        Cookie sessionCookie = getSessionCookie("tester", "password1234511");
        UrlRequestDto request = new UrlRequestDto();
        request.setLongUrl("https://www.longurl.com");
        MvcResult result = mockMvc.perform(post("/api/v1/short-urls")
                .cookie(sessionCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        String responseBody = result.getResponse().getContentAsString();
        UrlResponseDto responseDto = objectMapper.readValue(responseBody, UrlResponseDto.class);
        String shortCode = responseDto.getShortUrl().substring(responseDto.getShortUrl().lastIndexOf("/")+ 1);
        mockMvc.perform(get("/"+shortCode))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://www.longurl.com"));
    }
    @Test
    void shouldGetNoUrl()throws Exception{
        mockMvc.perform(get("/1111111"))
                .andExpect(status().isNotFound());

    }
}
