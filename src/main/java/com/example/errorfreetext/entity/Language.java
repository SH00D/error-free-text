package com.example.errorfreetext.entity;

public enum Language {
    EN("en"),
    RU("ru");

    private final String code;

    Language(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
