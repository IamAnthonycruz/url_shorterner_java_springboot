package com.cruz.url_shortener.controller;

import com.cruz.url_shortener.dto.UrlRequestDto;
import com.cruz.url_shortener.dto.UrlResponseDto;
import com.cruz.url_shortener.service.UrlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RequiredArgsConstructor
@RestController
public class UrlController {
    private final UrlService urlService;
    @PostMapping("/api/v1/short-url")
    ResponseEntity<UrlResponseDto>shortenUrl(@Valid @RequestBody UrlRequestDto urlRequestDto){
        var response = urlService.shortenUrl(urlRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    };
    @GetMapping("/{shortCode}")
    ResponseEntity<String>getLongUrl(@PathVariable String shortCode){
        var longUrl = urlService.getLongUrl(shortCode);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(longUrl)).build();
    }


}
