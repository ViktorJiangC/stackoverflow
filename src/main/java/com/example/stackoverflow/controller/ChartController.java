package com.example.stackoverflow.controller;

import com.example.stackoverflow.service.DataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/Chart")
public class ChartController {
    private final DataService dataService;

    @PostMapping("/test")
    public int test() {
        return dataService.test();
    }
}
