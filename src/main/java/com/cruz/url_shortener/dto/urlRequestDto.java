package com.cruz.url_shortener.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
public class urlRequestDto {
    @URL
    @NotBlank
    String longUrl;
}
