package com.cruz.url_shortener.controller;

import com.cruz.url_shortener.dto.UrlRequestDto;
import com.cruz.url_shortener.dto.UrlResponseDto;
import com.cruz.url_shortener.service.UrlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
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
    @GetMapping("/api/v1/short-urls/mine")
    ResponseEntity<List<UrlResponseDto>>getAllUserUrls(){
        Long userId = Long.valueOf(
                SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString()
        );
        var urlResponse = urlService.getAllUserUrls(userId);
        return ResponseEntity.status(HttpStatus.OK).body(urlResponse);
    }
    @DeleteMapping("/api/v1/short-urls/{shortCode}")
    ResponseEntity<Void>disableShortUrl(@PathVariable String shortCode){
        Long userId = Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString());
        urlService.disableShortUrl(shortCode, userId);
        return ResponseEntity.noContent().build();
    }


}
