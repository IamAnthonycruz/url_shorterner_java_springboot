package com.cruz.url_shortener.component;

public interface Encoder<EncoderType, DecoderType> {
     EncoderType encode(DecoderType decodable);
    DecoderType decode(EncoderType encodable);
}

