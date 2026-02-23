package com.cruz.url_shortener.repository;

import com.cruz.url_shortener.models.Url;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface urlRepository extends JpaRepository<Url, Long> {
    Optional<Url> findByLongUrl(String longUrl);
}
