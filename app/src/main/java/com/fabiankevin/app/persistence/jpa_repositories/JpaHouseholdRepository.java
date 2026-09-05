package com.fabiankevin.app.persistence.jpa_repositories;

import com.fabiankevin.app.persistence.entities.HouseholdEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JpaHouseholdRepository extends JpaRepository<HouseholdEntity, UUID> {
    @Query("SELECT DISTINCT h FROM HouseholdEntity h LEFT JOIN h.members m WHERE h.leaderId = :userId OR m.userId = :userId")
    List<HouseholdEntity> findByUserId(@Param("userId") UUID userId);

    @Query("""
            SELECT DISTINCT m.userId FROM HouseholdEntity h
            JOIN h.members m
            WHERE h.leaderId = :userId OR EXISTS (
                SELECT 1 FROM HouseholdEntity h2
                JOIN h2.members m2
                WHERE m2.userId = :userId)
            """)
    List<UUID> findHouseholdMemberUserIdsByLeaderId(@Param("userId") UUID userId);
}
