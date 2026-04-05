package com.cruz.url_shortener;

import com.cruz.url_shortener.config.BaseIntegrationTest;
import com.cruz.url_shortener.dto.LoginRequestDto;
import com.cruz.url_shortener.dto.RegistrationRequestDto;
import com.cruz.url_shortener.dto.UrlRequestDto;
import com.cruz.url_shortener.dto.UrlResponseDto;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class RedisTest extends BaseIntegrationTest {
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @BeforeEach
    void flushRedis(){
        stringRedisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
    }


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
    void shouldCacheOnRedirect() throws Exception{
        Cookie sessionCookie = getSessionCookie("user", "password12345!");
        UrlRequestDto urlRequestDto = new UrlRequestDto();
        urlRequestDto.setLongUrl("https://www.longurl.com");
        MvcResult res = mockMvc.perform(post("/api/v1/short-urls")
                .cookie(sessionCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(urlRequestDto)))
                .andExpect(status().isCreated()).andReturn();
        String responseBody = res.getResponse().getContentAsString();
        UrlResponseDto responseDto = objectMapper.readValue(responseBody, UrlResponseDto.class);
        String shortCode = responseDto.getShortUrl().substring(responseDto.getShortUrl().lastIndexOf("/")+1);
        assertFalse(stringRedisTemplate.hasKey(shortCode));


            MvcResult getRes = mockMvc.perform(get("/"+shortCode))
                    .andExpect(status().isFound())
                    .andReturn();
            var secondCacheHitTTL = stringRedisTemplate.getExpire(shortCode);
            assertTrue(stringRedisTemplate.hasKey(shortCode));
           assertEquals(secondCacheHitTTL, 3600);


    }
    @Test
    void shouldInvalidateCache() throws Exception{
        Cookie sessionCookie = getSessionCookie("user1", "password12345!");
        UrlRequestDto urlRequestDto = new UrlRequestDto();
        urlRequestDto.setLongUrl("https://www.longurl.com");
        MvcResult res = mockMvc.perform(post("/api/v1/short-urls")
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(urlRequestDto)))
                .andExpect(status().isCreated()).andReturn();
        String responseBody = res.getResponse().getContentAsString();
        UrlResponseDto responseDto = objectMapper.readValue(responseBody, UrlResponseDto.class);
        String shortCode = responseDto.getShortUrl().substring(responseDto.getShortUrl().lastIndexOf("/")+1);
        MvcResult getRes = mockMvc.perform(get("/"+shortCode))
                .andExpect(status().isFound())
                .andReturn();
        var secondCacheHitTTL = stringRedisTemplate.getExpire(shortCode);
        assertTrue(stringRedisTemplate.hasKey(shortCode));
        assertEquals(secondCacheHitTTL, 3600);
        mockMvc.perform(delete("/api/v1/short-urls/"+shortCode).cookie(sessionCookie))
                .andExpect(status().isOk());
        assertFalse(stringRedisTemplate.hasKey(shortCode));
        mockMvc.perform(get("/"+shortCode)).andExpect(status().isNotFound());

    }
}
