package com.cruz.url_shortener.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
@Data
public class RegistrationRequestDto {
    @NotBlank
    @Length(max = 50)
    private String userName;
    @Length(min=12 )
    @NotBlank
    private String password;
}
