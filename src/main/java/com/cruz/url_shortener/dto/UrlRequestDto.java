package com.cruz.url_shortener.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
public class UrlRequestDto {
    @URL
    @NotBlank
    String longUrl;
}
