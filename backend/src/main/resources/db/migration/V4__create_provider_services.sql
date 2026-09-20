CREATE TABLE provider_services (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    provider_id BIGINT UNSIGNED NOT NULL,
    name VARCHAR(150) NOT NULL,
    name_key VARCHAR(150) NOT NULL,
    description VARCHAR(1000) NULL,
    duration_minutes INT NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    active BIT(1) NOT NULL DEFAULT b'1',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_provider_services_provider (provider_id),
    KEY idx_provider_services_provider_active (provider_id, active),
    CONSTRAINT fk_provider_services_provider FOREIGN KEY (provider_id) REFERENCES providers (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
