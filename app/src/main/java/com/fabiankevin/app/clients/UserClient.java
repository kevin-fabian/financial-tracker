package com.fabiankevin.app.clients;

import com.fabiankevin.app.models.User;

public interface UserClient {
    User getUserByEmail(String email);
}
