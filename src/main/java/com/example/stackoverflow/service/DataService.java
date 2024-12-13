package com.example.stackoverflow.service;

import java.util.List;
import java.util.Map;

public interface DataService {
    int test();

    Map<String, Long> getDataSize();

    Map<String, Integer> getTopics();

    Map<String, Integer> getErrors();

    Map<String, Integer> getUsersDistribution();

    Map<String, Integer> getQuestionsDistribution();

    Map<String, Integer> getAnswersDistribution();

    Map<String, Integer> getProTopics();

    Map<String, Double> getAverageUserScores();

    Map<String, Map<String, Double>> calculateResponseTimeMetrics();

    Map<String, Double> getAverageScoresByLengthRange();
}
