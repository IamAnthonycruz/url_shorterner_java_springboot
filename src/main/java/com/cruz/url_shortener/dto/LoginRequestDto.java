package com.cruz.url_shortener.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequestDto {
    @NotBlank(message = "Please enter a valid username")
    private String userName;

    @NotBlank(message = "Please enter a valid password")
    private String password;
}
