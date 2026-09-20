package com.bookease.provider;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProviderRepository extends JpaRepository<Provider, Long> {

    Optional<Provider> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    boolean existsByCategoryId(Long categoryId);

    long countByCategoryId(Long categoryId);

    List<Provider> findByStatusOrderByCreatedAtAsc(ProviderStatus status);

    @Query("""
            select p from Provider p
            where p.status = com.bookease.provider.ProviderStatus.APPROVED
              and (:categoryId is null or p.category.id = :categoryId)
              and (:city is null or lower(p.city) = :city)
              and (:name is null or lower(p.businessName) like concat('%', :name, '%'))
            """)
    Page<Provider> searchApproved(
            @Param("categoryId") Long categoryId,
            @Param("city") String city,
            @Param("name") String name,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Provider p where p.id = :id")
    Optional<Provider> findByIdForUpdate(@Param("id") Long id);
}
