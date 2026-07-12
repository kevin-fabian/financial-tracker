package com.fabiankevin.app.persistence.jpa_repositories;

import com.fabiankevin.app.persistence.entities.SharedSpaceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaSharedSpaceRepository extends JpaRepository<SharedSpaceEntity, UUID> {
}
