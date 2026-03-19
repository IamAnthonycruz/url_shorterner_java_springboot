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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
    public LoginResponseDto login(LoginRequestDto loginRequestDto) {
        var user = userRepository.findByUserName(loginRequestDto.getUserName()).orElseThrow(()-> new InvalidCredentialException("Username or Password is Invalid")); //I will create this custome exception since we dont want to give any clues in case anyone gets lucky!
        var decryptedPassword = passwordEncoder.matches(loginRequestDto.getPassword(), user.getPassword());
        if (!decryptedPassword) {
            throw new InvalidCredentialException("Username or Password is Invalid");
        }

            UUID sessionId = UUID.randomUUID();
            stringRedisTemplate.opsForValue().set(String.valueOf(sessionId), String.valueOf(user.getId()),24, TimeUnit.HOURS);
    }
}
