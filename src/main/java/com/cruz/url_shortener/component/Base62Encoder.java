package com.cruz.url_shortener.component;

import org.springframework.stereotype.Component;

@Component
public class Base62Encoder implements LinkEncoder{
    private static final String lookUpString = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    @Override
    public String encode(Long id) {
        try {
            var shortCode = new StringBuilder();
            while (id > 0) {
                int remainder = (int) (id % 62);
                shortCode.append(lookUpString.charAt(remainder));
                id /=62;
            }
            return shortCode.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Long decode(String shortCode) {
        long id = 0;
        for(int i = shortCode.length()-1; i >=0; i --){
            int value = lookUpString.indexOf(shortCode.charAt(i));
            id = id * 62 + value;
        }
        return id;
    }
}
