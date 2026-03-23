package com.cruz.url_shortener.service.impl;

import com.cruz.url_shortener.component.Base62Encoder;
import com.cruz.url_shortener.config.AppProperties;
import com.cruz.url_shortener.dto.UrlRequestDto;
import com.cruz.url_shortener.dto.UrlResponseDto;
import com.cruz.url_shortener.entity.User;
import com.cruz.url_shortener.exception.ShortCodeNotFoundException;
import com.cruz.url_shortener.mapper.UrlMapper;
import com.cruz.url_shortener.repository.UrlRepository;
import com.cruz.url_shortener.repository.UserRepository;
import com.cruz.url_shortener.service.UrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.file.attribute.UserPrincipalNotFoundException;
import java.time.Duration;


@Service
@RequiredArgsConstructor
public class UrlServiceImpl implements UrlService {
    private final UrlRepository urlRepository;
    private final Base62Encoder base62Encoder;
    private final UrlMapper urlMapper;
    private final AppProperties appProperties;
    private final StringRedisTemplate stringRedisTemplate;
    private final UserRepository userRepository;

    @Override
    public UrlResponseDto shortenUrl(UrlRequestDto urlRequestDto, Long userId) {
            var user = userRepository.findById(userId).orElseThrow();
            var entity = urlRepository.findByLongUrlAndUser(urlRequestDto.getLongUrl(), user);
            if (entity.isPresent()) {
                if(!entity.get().isShortCodeActive()){
                    entity.get().setShortCodeActive(true);
                    urlRepository.save(entity.get());
                }
                var responseDto = urlMapper.toResponseDto(entity.get());
                responseDto.setShortUrl(appProperties.getBaseUrl()+"/"+entity.get().getShortCode());
                return responseDto;
            } else {
                var urlEntity = urlMapper.toEntity(urlRequestDto);
                urlEntity.setUser(user);
                urlRepository.save(urlEntity);

                var shortCode = base62Encoder.encode(urlEntity.getId());
                urlEntity.setShortCode(shortCode);

                urlRepository.save(urlEntity);

                var responseDto = urlMapper.toResponseDto(urlEntity);
                responseDto.setShortUrl(appProperties.getBaseUrl()+"/"+urlEntity.getShortCode());
                return responseDto;
            }

        }


    @Override
    public String getLongUrl(String shortCode) {
        String longUrl;

        var cachedUrl = stringRedisTemplate.opsForValue().get(shortCode);

        if(cachedUrl != null) {
            longUrl = cachedUrl;
        } else{
            var entity = urlRepository.findByShortCode(shortCode).orElseThrow(()-> new ShortCodeNotFoundException("Short Code Not Found: " + shortCode));
            if(!entity.isShortCodeActive()){
                throw new ShortCodeNotFoundException("Short Code Not Found: "+ shortCode);
            }
            longUrl = entity.getLongUrl();
            stringRedisTemplate.opsForValue().set(shortCode,longUrl, Duration.ofHours(1));
        }

        urlRepository.incrementHitCount(shortCode);
        return longUrl;
    }

    @Override
    public void disableShortUrl(String shortCode) {
        var entity = urlRepository.findByShortCode(shortCode).
                orElseThrow(()-> new ShortCodeNotFoundException("Short code not found: " + shortCode));
        entity.setShortCodeActive(false);
        urlRepository.save(entity);
        stringRedisTemplate.delete(shortCode);

    }
}
