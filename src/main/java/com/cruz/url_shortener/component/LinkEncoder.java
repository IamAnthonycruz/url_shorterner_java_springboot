package com.cruz.url_shortener.component;

public interface LinkEncoder {
    String encode(Long id);
    Long decode(String shortCode);
}
