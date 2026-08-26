package com.example.kidhackbackend.users.pairing.entity;

import com.example.kidhackbackend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "pairing_code")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PairingCode extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private Long parentId;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    public PairingCode(String code, Long parentId, LocalDateTime expiresAt) {
        this.code = code;
        this.parentId = parentId;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}