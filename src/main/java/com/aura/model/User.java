package com.aura.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String email;

    private String name;
    private String avatarUrl;
    private String googleId;

    @Column(length = 2048)
    private String accessToken;

    @Column(length = 2048)
    private String refreshToken;

    private Instant tokenExpiresAt;

    @Column(nullable = false)
    private String plan = "free";

    private Instant createdAt = Instant.now();

    private String password;
    private String provider;
    // ← NO manual getters/setters needed, @Data handles everything
}