package com.example.stackoverflow.service;

import java.util.Map;

public interface DataService {

    int search(String keyword);

    Map<String, Long> getDataSize();

    Map<String, Integer> getTopics(int n);

    Map<String, Integer> getErrors(int n);

    Map<String, Integer> getUsersDistribution();

    Map<String, Integer> getQuestionsDistribution();

    Map<String, Integer> getAnswersDistribution();

    Map<String, Integer> getProTopics(int n);

    Map<String, Double> getAverageUserScores();

    Map<String, Map<String, Double>> calculateResponseTimeMetrics();

    Map<String, Double> getAverageScoresByLengthRange();
}
