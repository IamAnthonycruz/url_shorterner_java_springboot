package com.cruz.url_shortener;

import com.cruz.url_shortener.config.BaseIntegrationTest;
import com.cruz.url_shortener.entity.Url;
import com.cruz.url_shortener.entity.User;
import com.cruz.url_shortener.repository.UrlRepository;
import com.cruz.url_shortener.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.Time;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UrlRepositoryTest extends BaseIntegrationTest {
    @Autowired
     UrlRepository urlRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    private TestEntityManager testEntityManager;
    private User user;
    @BeforeEach
    void setUp(){
        user = new User();
        user.setUserName("myusername");
        user.setPassword("password");
        userRepository.save(user);
    }
    @Test
    void shouldThrowWhenDuplicateShortCode(){
        Url url1 = new Url();
        url1.setLongUrl("https://first.com");
        url1.setShortCode("1");
        url1.setUser(user);
        urlRepository.saveAndFlush(url1);
        Url url2 = new Url();
        url2.setLongUrl("https://second.com");
        url2.setShortCode("1");
        url2.setUser(user);

        assertThrows(DataIntegrityViolationException.class, () -> {
            urlRepository.saveAndFlush(url2);
        });
    }
    @Test
    void shouldThrowWhenDuplicateUsername(){
        User user1 = new User();
        user1.setUserName("user");
        user1.setPassword("hi");
        user1.setCreatedAt(Instant.now());
        userRepository.saveAndFlush(user1);
        User user2 = new User();
        user2.setUserName("user");
        user2.setPassword("hi");
        user2.setCreatedAt(Instant.now());

        assertThrows(DataIntegrityViolationException.class, () -> {
            userRepository.saveAndFlush(user2);
        });
    }
    @Test
    void shouldReturnUrlWhenShortCodeExists(){
        String shortCode = "xyz789";
        Url url = new Url();
        url.setLongUrl("https://target-website.com");
        url.setShortCode(shortCode);
        url.setUser(user);
        urlRepository.saveAndFlush(url);

        Optional<Url> foundUrl = urlRepository.findByShortCode(shortCode);
        assertTrue(foundUrl.isPresent(), "url should be found");
        assertEquals("https://target-website.com", foundUrl.get().getLongUrl());
        assertEquals(shortCode, foundUrl.get().getShortCode());
    }
    @Test
    void shouldReturnEmptyOptionalWhenShortCodeDoesNotExist(){
        Optional<Url> foundUrl = urlRepository.findByShortCode("nonexistent");
        assertTrue(foundUrl.isEmpty(), "should be empty");
    }
    @Test
    void shouldIncrementHitCountAtomically(){
        String shortCode = "hit123";
        Url url = new Url();
        url.setLongUrl("http://example.com");
        url.setShortCode(shortCode);
        url.setUser(user);
        urlRepository.saveAndFlush(url);
        urlRepository.incrementHitCount(shortCode);
        testEntityManager.flush();
        testEntityManager.clear();
        Url updatedUrl = urlRepository.findByShortCode(shortCode).orElseThrow();
        assertEquals(1, updatedUrl.getHitCount());
    }
    @Test
    void shouldReturnUrlsByUserId(){
        Url url = new Url();
        url.setLongUrl("Https://association-test.com");
        url.setShortCode("user123");
        url.setUser(user);
        urlRepository.saveAndFlush(url);

        List<Url> userUrls = urlRepository.findAllByUserId(user.getId());

        assertEquals(1, userUrls.size(), "User should have exactly 1 URL");
        Url retrievedUrl = userUrls.get(0);
        assertEquals("Https://association-test.com", retrievedUrl.getLongUrl());
        assertNotNull(retrievedUrl.getUser(), "User not null");
        assertEquals(user.getId(), retrievedUrl.getUser().getId());
    }
}
