package com.fabiankevin.app.clients;

import com.fabiankevin.app.models.User;

import java.util.List;
import java.util.UUID;

public interface UserClient {
    User getUserByEmail(String email);

    List<User> getUsersByIds(List<UUID> userIds);
}
