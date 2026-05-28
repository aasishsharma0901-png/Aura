package com.aura.model;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SubscriberRepository extends JpaRepository<Subscriber, String> {
    Optional<Subscriber> findByEmail(String email);
}