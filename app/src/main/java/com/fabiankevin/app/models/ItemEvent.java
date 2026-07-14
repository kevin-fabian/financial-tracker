package com.fabiankevin.app.models;

import com.fabiankevin.app.models.enums.EventAction;

import java.util.UUID;

public record ItemEvent<T>(
    UUID usedId,
    EventAction action,
    T data) {}