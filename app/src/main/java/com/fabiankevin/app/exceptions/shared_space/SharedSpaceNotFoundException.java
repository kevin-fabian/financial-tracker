package com.fabiankevin.app.exceptions.shared_space;

import com.fabiankevin.app.exceptions.NotFoundException;

public class SharedSpaceNotFoundException extends NotFoundException {
    public SharedSpaceNotFoundException() {
        super("Shared space not found");
    }
}
