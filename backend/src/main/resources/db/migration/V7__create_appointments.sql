CREATE TABLE appointments (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    provider_id BIGINT UNSIGNED NOT NULL,
    service_id BIGINT UNSIGNED NOT NULL,
    start_at TIMESTAMP(6) NOT NULL,
    end_at TIMESTAMP(6) NOT NULL,
    status VARCHAR(32) NOT NULL,
    notes VARCHAR(1000) NULL,
    idempotency_key VARCHAR(80) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_appointments_user_idempotency (user_id, idempotency_key),
    KEY idx_appointments_provider_start (provider_id, start_at),
    KEY idx_appointments_user_start (user_id, start_at),
    KEY idx_appointments_status (status),
    KEY idx_appointments_provider_status_start (provider_id, status, start_at),
    CONSTRAINT fk_appointments_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_appointments_provider FOREIGN KEY (provider_id) REFERENCES providers (id),
    CONSTRAINT fk_appointments_service FOREIGN KEY (service_id) REFERENCES provider_services (id),
    CONSTRAINT chk_appointments_status CHECK (status IN
        ('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED', 'NO_SHOW'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
