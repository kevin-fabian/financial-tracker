package com.fabiankevin.app.models.constants;

public final class AppValidationRules {
    private AppValidationRules(){}

    public static final int USERNAME_MIN_LENGTH = 3;
    public static final int USERNAME_MAX_LENGTH = 50;
    public static final String USERNAME_REGEX = "^[^@\\s]+@(yopmail|gmail|yahoo|outlook|hotmail|icloud|protonmail|aol)\\.(com|net|org)$";
    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int PASSWORD_MAX_LENGTH = 100;
    public static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$";
    public static final String NAME_REGEX = "^[a-zA-Z\\s'-.]+$";
}
