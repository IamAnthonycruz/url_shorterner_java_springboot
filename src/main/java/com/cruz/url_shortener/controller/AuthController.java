package com.cruz.url_shortener.controller;

import com.cruz.url_shortener.dto.LoginRequestDto;
import com.cruz.url_shortener.dto.LoginResponseDto;
import com.cruz.url_shortener.dto.RegistrationRequestDto;
import com.cruz.url_shortener.dto.RegistrationResponseDto;
import com.cruz.url_shortener.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    @PostMapping("/api/v1/auth/register")
    ResponseEntity<RegistrationResponseDto>register(@Valid @RequestBody RegistrationRequestDto registrationRequestDto){
        RegistrationResponseDto registrationResponseDto = authService.registerUser(registrationRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(registrationResponseDto);
    }
    @PostMapping("/api/v1/auth/login")
    ResponseEntity<LoginResponseDto>login(
            @CookieValue(name = "session-id", defaultValue = "") String sessionIdCookie,
            @Valid @RequestBody LoginRequestDto loginRequestDto){

            var sessionId = authService.login(sessionIdCookie,loginRequestDto);
            var newSessionIdCookie = ResponseCookie.from("session-id", String.valueOf(sessionId))
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofHours(24))
                .build();
        return ResponseEntity.status(HttpStatus.OK).header(HttpHeaders.SET_COOKIE, newSessionIdCookie.toString()).body(new LoginResponseDto(loginRequestDto.getUserName()));
    }
    @PostMapping("/api/v1/auth/logout")
    ResponseEntity<Void>logout(
            @CookieValue(name = "session-id", defaultValue = "") String sessionIdCookie
    ){

        authService.logout(sessionIdCookie);
        var newSessionIdCookie = ResponseCookie.from("session-id", String.valueOf(sessionIdCookie))
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, newSessionIdCookie.toString()).build();
    }
}
