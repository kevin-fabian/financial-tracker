package com.fabiankevin.app.clients;

import com.fabiankevin.app.clients.dtos.CreateUserRequest;
import com.fabiankevin.app.clients.dtos.UserResponse;

public interface UserClient {
    UserResponse createUser(CreateUserRequest request);
}
