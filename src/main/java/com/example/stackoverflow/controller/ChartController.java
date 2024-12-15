package com.example.stackoverflow.controller;

import com.example.stackoverflow.service.DataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/Chart")
public class ChartController {
    private final DataService dataService;

    @PostMapping("/test")
    public int test() {
        return dataService.test();
    }

    @PostMapping("/getDataSize")
    public Map<String, Long> getDataSize() {
        return dataService.getDataSize();
    }

    @PostMapping("/getTopics")
    public Map<String, Integer> getTopics() {
        return dataService.getTopics();
    }

    @PostMapping("/getErrors")
    public Map<String, Integer> getErrors() {
        return dataService.getErrors();
    }

    @PostMapping("/getUsersDistribution")
    public Map<String, Integer> getUsersDistribution() {
        return dataService.getUsersDistribution();
    }
}
