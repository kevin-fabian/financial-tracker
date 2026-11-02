package com.fabiankevin.app.exceptions;

public class SyncStateNotFoundException extends NotFoundException {
    public SyncStateNotFoundException() {
        super("Sync state not found");
    }
}
