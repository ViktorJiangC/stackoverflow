package com.example.stackoverflow.service;

import lombok.Getter;

@Getter
public enum JavaTopic {
    GENERICS("generics", "generics", "generic class"),
    COLLECTIONS("collection", "List", "Map"),
    IO("I/O", "file", "BufferedReader"),
    LAMBDA("lambda", "Functional", "Predicate"),
    MULTITHREADING("threading", "thread", "concurrent"),
    SOCKET("socket", "network", "HTTP"),
    ANNOTATIONS("annotation", "@Override", "@Deprecated"),
    DATABASE("database", "JDBC", "SQL");

    private final String topicName;
    private final String[] keywords;

    JavaTopic(String topicName, String... keywords) {
        this.topicName = topicName;
        this.keywords = keywords;
    }

}