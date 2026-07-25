package com.fabiankevin.app.exceptions;

public class BudgetNotFoundException extends NotFoundException {
    public BudgetNotFoundException() {
        super("Budget not found");
    }
}
