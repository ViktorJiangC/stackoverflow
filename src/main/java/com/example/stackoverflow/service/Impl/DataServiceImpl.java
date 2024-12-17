package com.example.stackoverflow.service.Impl;

import com.example.stackoverflow.model.Answer;
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
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DataServiceImpl implements DataService {
    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final Map<String, Set<Integer>> topicQuestionMap = new ConcurrentHashMap<>();
    private final Map<String, Set<Integer>> errorQuestionMap = new ConcurrentHashMap<>();
    private final Set<User> proUsers = new HashSet<>();
    private final Map<String, Integer> proTopics = new LinkedHashMap<>();
    private final List<Question> closedQuestions = new ArrayList<>();

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

        // 初始化 Pro Users 数据
        proUsers.addAll(userRepository.findUsersByScoreGreaterThan(200));

        try {
            executorService.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread execution interrupted", e);
        } finally {
            executorService.shutdown();
        }

        // 计算 Pro Topics
        calculateProTopics();

        closedQuestions.addAll(questionRepository.findQuestionsByClosedDateIsNotNullAndCreationDateIsNotNull());
    }

    private void calculateProTopics() {
        List<Answer> proAnswers = answerRepository.findAnswersByOwnerUserIsIn(proUsers);
        // 获取 pro 用户回答的所有问题 ID
        List<Integer> proQuestions = proAnswers.stream()
                .map(Answer::getQuestion)
                .map(Question::getId)
                .toList();

        ExecutorService executorService = Executors.newFixedThreadPool(16);
        List<Callable<Map.Entry<String, Integer>>> tasks = topicQuestionMap.entrySet().stream()
                .map(entry -> (Callable<Map.Entry<String, Integer>>) () -> {
                    long count = entry.getValue().stream()
                            .filter(proQuestions::contains)
                            .count();
                    return new AbstractMap.SimpleEntry<>(entry.getKey(), (int) count);
                })
                .collect(Collectors.toList());

        try {
            List<Future<Map.Entry<String, Integer>>> results = executorService.invokeAll(tasks);
            for (Future<Map.Entry<String, Integer>> future : results) {
                Map.Entry<String, Integer> entry = future.get();
                proTopics.put(entry.getKey(), entry.getValue());
            }
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread execution interrupted", e);
        } finally {
            executorService.shutdown();
        }
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

    @Override
    public Map<String, Integer> getProTopics() {
        return Collections.unmodifiableMap(
                proTopics.entrySet()
                        .stream()
                        .sorted((entry1, entry2) -> Integer.compare(entry2.getValue(), entry1.getValue())) // 降序排序
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (e1, e2) -> e1,
                                LinkedHashMap::new // 保持插入顺序
                        ))
        );
    }


}