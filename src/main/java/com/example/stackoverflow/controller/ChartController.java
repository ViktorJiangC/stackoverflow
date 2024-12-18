package com.example.stackoverflow.controller;

import com.example.stackoverflow.service.DataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/Chart")
public class ChartController {
    private final DataService dataService;

    @GetMapping("/getDataSize")
    public Map<String, Long> getDataSize() {
        return dataService.getDataSize();
    }

    @GetMapping("/getTopics")
    public Map<String, Integer> getTopics() {
        return dataService.getTopics();
    }

    @GetMapping("/getErrors")
    public Map<String, Integer> getErrors() {
        return dataService.getErrors();
    }

    @GetMapping("/getUsersDistribution")
    public Map<String, Integer> getUsersDistribution() {
        return dataService.getUsersDistribution();
    }

    @GetMapping("/getQuestionsDistribution")
    public Map<String, Integer> getQuestionsDistribution() {
        return dataService.getQuestionsDistribution();
    }

    @GetMapping("/getAnswersDistribution")
    public Map<String, Integer> getAnswersDistribution() {
        return dataService.getAnswersDistribution();
    }

    @GetMapping("/getProTopics")
    public Map<String, Integer> getProTopics() {
        return dataService.getProTopics();
    }

    @GetMapping("/getAverageUserScores")
    public Map<String, Double> getAverageUserScores() {
        return dataService.getAverageUserScores();
    }

    @GetMapping("/calculateResponseTimeMetrics")
    public Map<String, Map<String, Double>> calculateResponseTimeMetrics() {
        return dataService.calculateResponseTimeMetrics();
    }

    @GetMapping("/getAverageScoresByLengthRange")
    public Map<String, Double> getAverageScoresByLengthRange() {
        return dataService.getAverageScoresByLengthRange();
    }
}
