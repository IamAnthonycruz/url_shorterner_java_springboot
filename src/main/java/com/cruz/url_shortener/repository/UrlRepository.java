package com.cruz.url_shortener.repository;

import com.cruz.url_shortener.entity.Url;
import com.cruz.url_shortener.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UrlRepository extends JpaRepository<Url, Long> {
    Optional<Url> findByLongUrlAndUser(String longUrl, User user);
    Optional<Url> findByShortCode(String shortCode);
    @Transactional
    @Modifying
    @Query("Update Url u SET u.hitCount = u.hitCount+ 1 WHERE u.shortCode = :shortCode")
    void incrementHitCount(@Param("shortCode") String shortCode);
}
