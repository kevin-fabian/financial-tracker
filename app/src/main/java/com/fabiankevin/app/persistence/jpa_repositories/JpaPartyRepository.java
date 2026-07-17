package com.fabiankevin.app.persistence.jpa_repositories;

import com.fabiankevin.app.persistence.entities.PartyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JpaPartyRepository extends JpaRepository<PartyEntity, UUID> {
    @Query("SELECT DISTINCT s FROM PartyEntity s LEFT JOIN s.partyMembers p WHERE s.partyLeaderId = :playerId OR p.playerId = :playerId")
    List<PartyEntity> findByPlayerId(@Param("playerId") UUID playerId);

    @Query("""
            SELECT DISTINCT p.playerId FROM PartyEntity s
            JOIN s.partyMembers p
            WHERE s.id IN (
                SELECT s2.id FROM PartyEntity s2 LEFT JOIN s2.partyMembers p2
                WHERE s2.partyLeaderId = :playerId OR p2.playerId = :playerId)
            """)
    List<UUID> findPartyMemberPlayerIdsByPlayerId(@Param("playerId") UUID playerId);
}
