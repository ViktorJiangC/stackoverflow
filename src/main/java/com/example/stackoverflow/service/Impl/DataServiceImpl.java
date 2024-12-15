package com.example.stackoverflow.service.Impl;

import com.example.stackoverflow.model.Question;
import com.example.stackoverflow.model.User;
import com.example.stackoverflow.repository.AnswerRepository;
import com.example.stackoverflow.repository.QuestionRepository;
import com.example.stackoverflow.repository.UserRepository;
import com.example.stackoverflow.service.DataService;
import com.example.stackoverflow.service.JavaError;
import com.example.stackoverflow.service.JavaTopic;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class DataServiceImpl implements DataService {
    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final Map<String, Set<Integer>> topicQuestionMap = new ConcurrentHashMap<>();
    private final Map<String, Set<Integer>> errorQuestionMap = new ConcurrentHashMap<>();
    private final Set<User> proUsers = new HashSet<>();

    @Override
    public int test() {
        return answerRepository.findAnswerByBodyContaining("test").size();
    }

    @Override
    public Map<String, Long> getDataSize() {
        return Map.of("answer", answerRepository.count(),
                "question", questionRepository.count(),
                "user", userRepository.count());
    }


    @PostConstruct
    public void initialize() {
        ExecutorService executorService = Executors.newFixedThreadPool(16);
        List<Callable<Void>> tasks = new ArrayList<>();
        // 初始化 Topics 数据
        for (JavaTopic topic : JavaTopic.values()) {
            tasks.add(() -> {
                Set<Integer> questionIds = ConcurrentHashMap.newKeySet();
                for (String keyword : topic.getKeywords()) {
                    List<Question> matchingQuestions = questionRepository.findQuestionsByBodyContainingIgnoreCaseOrTitleContainingIgnoreCaseOrTagContainingIgnoreCase(
                            keyword, keyword, keyword);
                    matchingQuestions.forEach(question -> questionIds.add(question.getId()));
                }
                topicQuestionMap.put(topic.getTopicName(), questionIds);
                return null;
            });
        }

        // 初始化 Errors 数据
        for (JavaError error : JavaError.values()) {
            tasks.add(() -> {
                Set<Integer> questionIds = ConcurrentHashMap.newKeySet();
                for (String keyword : error.getKeywords()) {
                    List<Question> matchingQuestions = questionRepository.findQuestionsByBodyContainingIgnoreCaseOrTitleContainingIgnoreCaseOrTagContainingIgnoreCase(
                            keyword, keyword, keyword);
                    matchingQuestions.forEach(question -> questionIds.add(question.getId()));
                }
                errorQuestionMap.put(error.getErrorName(), questionIds);
                return null;
            });
        }

        try {
            executorService.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread execution interrupted", e);
        } finally {
            executorService.shutdown();
        }

        proUsers.addAll(userRepository.findUsersByScoreGreaterThan(200));
    }

    @Override
    public Map<String, Integer> getTopics() {
        return topicQuestionMap.entrySet()
                .stream()
                .sorted((entry1, entry2) -> Integer.compare(entry2.getValue().size(), entry1.getValue().size())) // Sorting by size in descending order
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().size(),
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    @Override
    public Map<String, Integer> getErrors() {
        return errorQuestionMap.entrySet()
                .stream()
                .sorted((entry1, entry2) -> Integer.compare(entry2.getValue().size(), entry1.getValue().size())) // Sorting by size in descending order
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().size(),
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    @Override
    public Map<String, Integer> getUsersDistribution() {
        List<Object[]> result = userRepository.findUserScoreRanges();
        Map<String, Integer> scoreRangeMap = new TreeMap<>(new Comparator<String>() {
            @Override
            public int compare(String range1, String range2) {
                // Parse the lower bound of each range
                int lower1 = parseLowerBound(range1);
                int lower2 = parseLowerBound(range2);
                return Integer.compare(lower1, lower2);
            }

            // Helper method to extract the lower bound from the range string
            private int parseLowerBound(String range) {
                if (range.equals("Below 0")) {
                    return Integer.MIN_VALUE;  // "Below 0" should come first
                } else if (range.equals("Above 100")) {
                    return Integer.MAX_VALUE; // "Above 100" should come last
                } else {
                    String[] parts = range.split(" and ");
                    return Integer.parseInt(parts[0].replaceAll("[^0-9]", ""));
                }
            }
        });

        // Populate the TreeMap with the query results
        for (Object[] row : result) {
            String scoreRange = (String) row[0];
            Integer userCount = ((Number) row[1]).intValue();  // Convert from Number to Integer
            scoreRangeMap.put(scoreRange, userCount);
        }

        return scoreRangeMap;
    }

}