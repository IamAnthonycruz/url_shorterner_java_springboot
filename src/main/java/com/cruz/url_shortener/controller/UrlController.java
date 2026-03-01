package com.cruz.url_shortener.controller;

import com.cruz.url_shortener.dto.UrlRequestDto;
import com.cruz.url_shortener.dto.UrlResponseDto;
import com.cruz.url_shortener.service.UrlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class UrlController {
    private UrlService urlService;
    @PostMapping("/api/v1/short-url")
    ResponseEntity<UrlResponseDto>addUrl(@Valid @RequestBody UrlRequestDto urlRequestDto){
        var response = urlService.shortenUrl(urlRequestDto);
        return ResponseEntity.ok().body(response);
    };

}
