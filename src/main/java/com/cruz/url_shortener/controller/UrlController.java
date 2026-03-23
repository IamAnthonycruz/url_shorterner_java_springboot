package com.cruz.url_shortener.controller;

import com.cruz.url_shortener.dto.UrlRequestDto;
import com.cruz.url_shortener.dto.UrlResponseDto;
import com.cruz.url_shortener.service.UrlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Objects;

@RequiredArgsConstructor
@RestController
public class UrlController {
    private final UrlService urlService;
    @PostMapping("/api/v1/short-urls")
    ResponseEntity<UrlResponseDto>shortenUrl(@Valid @RequestBody UrlRequestDto urlRequestDto){
        Long userId = Long.valueOf(
                SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString()
        );
        var response = urlService.shortenUrl(urlRequestDto,userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    };
    @GetMapping("/{shortCode}")
    ResponseEntity<String>getLongUrl(@PathVariable String shortCode){

        var longUrl = urlService.getLongUrl(shortCode);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(longUrl)).build();
    }
    @DeleteMapping("/api/v1/short-urls/{shortCode}")
    ResponseEntity<String>disableShortUrl(@PathVariable String shortCode){
        urlService.disableShortUrl(shortCode);
        return ResponseEntity.noContent().build();
    }

}
