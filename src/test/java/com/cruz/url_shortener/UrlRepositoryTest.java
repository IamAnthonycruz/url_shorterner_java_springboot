package com.cruz.url_shortener;

import com.cruz.url_shortener.config.BaseIntegrationTest;
import com.cruz.url_shortener.entity.User;
import com.cruz.url_shortener.repository.UrlRepository;
import com.cruz.url_shortener.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UrlRepositoryTest extends BaseIntegrationTest {
    @Autowired
     UrlRepository urlRepository;
    @Autowired
    UserRepository userRepository;
    User user = new User();
    @BeforeEach
    void setUp(){
        user.setUserName("myusername");
        user.setPassword("password");
    }
    @Test
    void testUrlRepository(){
        userRepository.save(user);
    }
}
