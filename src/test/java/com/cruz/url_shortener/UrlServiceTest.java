package com.cruz.url_shortener;

import com.cruz.url_shortener.component.Base62Encoder;
import com.cruz.url_shortener.config.AppProperties;
import com.cruz.url_shortener.entity.Url;
import com.cruz.url_shortener.mapper.UrlMapper;
import com.cruz.url_shortener.repository.UrlRepository;
import com.cruz.url_shortener.repository.UserRepository;
import com.cruz.url_shortener.service.impl.UrlServiceImpl;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;


import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.any;
@ExtendWith(MockitoExtension.class)
public class UrlServiceTest {
    @Mock
    private UrlRepository urlRepository;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private Base62Encoder base62Encoder;
    @Mock
    private AppProperties appProperties;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UrlMapper urlMapper;
    @InjectMocks
    private UrlServiceImpl urlService;
    @Test
    void shouldReturnMessageFromCacheWhenCacheHit(){
        String key = "1";
        String expectedMessage = "https://example.come";

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn(expectedMessage);

        String result = urlService.getLongUrl(key);
        assertEquals(expectedMessage, result);
        verify(urlRepository, never()).findByShortCode("1");
    }
    @Test
    void shouldNotReturnMessageFromCacheWhenCacheHit(){
        String key = "1";
        String expectedMessage = "https://example.come";
        Url url = new Url();
        url.setLongUrl(expectedMessage);
        url.setShortCode(key);
        url.setShortCodeActive(true);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn(null);
        when(urlRepository.findByShortCode(key)).thenReturn(Optional.of(url));
        String result = urlService.getLongUrl(key);
        assertEquals(expectedMessage, result);
        verify(valueOperations).set(eq(key), eq(expectedMessage), any(Duration.class));
    }


}
