package com.cruz.url_shortener.models;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "url")
public class Url {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "long_url", nullable = false, columnDefinition = "TEXT")
    private String longUrl;


    @Column(name = "short_url", unique = true, nullable = false, length = 10)
    private String shortUrl;

    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "hit_count")
    private int hit_count = 0;

}
