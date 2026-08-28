package com.example.kidhackbackend.users.wallet.repository;

import com.example.kidhackbackend.users.wallet.entity.PgWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PgWebhookEventRepository extends JpaRepository<PgWebhookEvent, Long> {
}
