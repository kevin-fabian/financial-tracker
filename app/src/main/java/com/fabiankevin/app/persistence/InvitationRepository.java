package com.fabiankevin.app.persistence;

import com.fabiankevin.app.models.shared_space.Invitation;

import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository {
    Invitation save(Invitation invitation);

    Optional<Invitation> findById(UUID id);
}
