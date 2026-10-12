package com.example.errorfreetext.util;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class SpellerOptionsResolver {

    private static final int IGNORE_DIGITS = 2;
    private static final int IGNORE_URLS = 4;

    private static final Pattern DIGITS = Pattern.compile("\\d");
    private static final Pattern URL = Pattern.compile(
            "(?i)\\b(?:https?|ftp)://\\S+"
                    + "|\\bwww\\.\\S+"
                    + "|\\b[a-z0-9-]+(?:\\.[a-z0-9-]+)*\\.(?:com|ru|org|net|io|info|biz|dev|edu|gov|me|su|рф)\\b(?:/\\S*)?");

    public int resolve(String text) {
        int options = 0;
        if (DIGITS.matcher(text).find()) {
            options = options + IGNORE_DIGITS;
        }
        if (URL.matcher(text).find()) {
            options = options + IGNORE_URLS;
        }
        return options;
    }
}
