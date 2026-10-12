package com.example.errorfreetext.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SpellError(int code, int pos, int row, int col, int len, String word, List<String> s) {
}
