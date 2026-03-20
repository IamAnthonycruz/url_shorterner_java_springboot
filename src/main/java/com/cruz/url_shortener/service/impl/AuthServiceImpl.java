package com.cruz.url_shortener.service.impl;

import com.cruz.url_shortener.dto.LoginRequestDto;
import com.cruz.url_shortener.dto.LoginResponseDto;
import com.cruz.url_shortener.dto.RegistrationRequestDto;
import com.cruz.url_shortener.dto.RegistrationResponseDto;
import com.cruz.url_shortener.entity.User;
import com.cruz.url_shortener.exception.InvalidCredentialException;
import com.cruz.url_shortener.exception.UserAlreadyCreatedException;
import com.cruz.url_shortener.mapper.RegistrationMapper;
import com.cruz.url_shortener.repository.UserRepository;
import com.cruz.url_shortener.service.AuthService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RegistrationMapper registrationMapper;
    private final StringRedisTemplate stringRedisTemplate;
    @Override
    public RegistrationResponseDto registerUser(RegistrationRequestDto registrationRequestDto) {
        Optional<User> user = userRepository.findByUserName(registrationRequestDto.getUserName());
        if (user.isPresent()) {
            throw new UserAlreadyCreatedException("User already exists");}
            var newUser = registrationMapper.toEntity(registrationRequestDto);
            newUser.setPassword(passwordEncoder.encode(registrationRequestDto.getPassword()));
            userRepository.save(newUser);

            return registrationMapper.toResponseDto(newUser);

        }

    @Override
    public UUID login(String sessionIdCookie, LoginRequestDto loginRequestDto) {
        var user = userRepository.findByUserName(loginRequestDto.getUserName()).orElseThrow(()-> new InvalidCredentialException("Username or Password is Invalid")); //I will create this custom exception since we dont want to give any clues in case anyone gets lucky!
        var doesPasswordMatch = passwordEncoder.matches(loginRequestDto.getPassword(), user.getPassword());
        if (!doesPasswordMatch) {
            throw new InvalidCredentialException("Username or Password is Invalid");
        }
        if(!sessionIdCookie.isEmpty()) {
            stringRedisTemplate.opsForValue().getAndDelete(sessionIdCookie);
        }
        UUID sessionId = UUID.randomUUID();
        stringRedisTemplate.opsForValue().set(String.valueOf(sessionId), String.valueOf(user.getId()),24, TimeUnit.HOURS);
        return sessionId;
    }

    @Override
    public void logout(String sessionIdCookie) {
        stringRedisTemplate.opsForValue().getAndDelete(sessionIdCookie);
    }
}
