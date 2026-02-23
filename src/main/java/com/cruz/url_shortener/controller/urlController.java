package com.cruz.url_shortener.controller;

import com.cruz.url_shortener.dtos.urlRequestDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class urlController {
    @PostMapping("/api/v1/short_url")
    ResponseEntity<String>addUrl(@Valid @RequestBody urlRequestDto urlRequestDto){

        return ResponseEntity.ok().body("");
    };
}
