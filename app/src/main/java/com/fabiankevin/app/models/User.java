package com.fabiankevin.app.models;

import com.fabiankevin.app.models.enums.UserStatus;
import lombok.Builder;

import java.util.*;

@Builder
public record User(
        UUID id,
        String firstName,
        String lastName,
        UserStatus status,
        String mobileNumber,
        String email,
        String username,
        Locale locale,
        List<Account> accounts) {

    public User {
        if(accounts == null ) {
            accounts = new ArrayList<>();
        }
    }

    public void addAccount(Account account){
        Optional.ofNullable(account).orElseThrow(() -> new IllegalArgumentException("Account is required"));
        accounts.add(account);
    }
}
