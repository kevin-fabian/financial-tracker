package com.fabiankevin.app.persistence.jpa_repositories;

import com.fabiankevin.app.persistence.entities.SharedSpaceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JpaSharedSpaceRepository extends JpaRepository<SharedSpaceEntity, UUID> {
    @Query("SELECT DISTINCT s FROM SharedSpaceEntity s LEFT JOIN s.participants p WHERE s.ownerUserId = :userId OR p.userId = :userId")
    List<SharedSpaceEntity> findByUserId(@Param("userId") UUID userId);
}
