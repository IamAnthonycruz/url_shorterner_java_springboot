package com.cruz.url_shortener.controller;

import com.cruz.url_shortener.dto.RegistrationRequestDto;
import com.cruz.url_shortener.dto.RegistrationResponseDto;
import com.cruz.url_shortener.service.AuthService;
import jakarta.validation.Valid;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    @PostMapping("/api/v1/auth/register")
    ResponseEntity<RegistrationResponseDto>register(@Valid @RequestBody RegistrationRequestDto registrationRequestDto){
        RegistrationResponseDto registrationResponseDto = authService.registerUser(registrationRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(registrationResponseDto);
    }
}
