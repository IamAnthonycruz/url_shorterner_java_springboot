package com.cruz.url_shortener.entity;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;


@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "urls", indexes = {
        @Index(name = "idx_short_code", columnList = "short_code")
})
public class Url {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "long_url", nullable = false, columnDefinition = "TEXT")
    private String longUrl;


    @Column(name = "short_code", unique = true, nullable = false, length = 10)
    private String shortCode;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = true)
    private Instant createdAt;

    @Column(name = "hit_count", nullable = false)
    private long hitCount = 0;

}
