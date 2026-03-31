package com.cruz.url_shortener;

import com.cruz.url_shortener.component.Base62Encoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Base62EncoderTest {
    private Base62Encoder base62Encoder;

    @BeforeEach
    void setUp(){
        base62Encoder = new Base62Encoder();
    }

    @Test
    void shouldBase62Encode1(){
        String result = base62Encoder.encode(1L);
        assertEquals("1", result);
    }
    @Test
    void shouldBase62Encode62(){
        String result = base62Encoder.encode(62L);
        assertEquals("01",result);
    }
    @Test
    void shouldBase62Encode0(){
        String result = base62Encoder.encode(0L);
        assertEquals("", result);
    }
    @Test
    void shouldBase62EncodeDeterministic(){
        Long id = 99L;
        String res1 = base62Encoder.encode(id);
        String res2 = base62Encoder.encode(id);
        assertEquals(res1,res2);
    }
}
