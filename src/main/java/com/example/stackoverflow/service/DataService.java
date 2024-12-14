package com.example.stackoverflow.service;

import java.util.Map;

public interface DataService {
    int test();

    Map<String, Long> getDataSize();

    Map<String, Long> getTopics();
}
