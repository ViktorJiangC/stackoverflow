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
    private final UserRepository userRepository;;

    @Override
    public int search(String keyword) {
        for(JavaTopic topic : JavaTopic.values()) {
            if(topic.getTopicName().equals(keyword)){
                Set<Integer> questionIds = ConcurrentHashMap.newKeySet();
                for (String key : topic.getKeywords()) {
                    List<Question> matchingQuestions = questionRepository.findQuestionsByBodyContainingIgnoreCaseOrTitleContainingIgnoreCaseOrTagContainingIgnoreCase(
                            key, key, key);
                    matchingQuestions.forEach(question -> questionIds.add(question.getId()));
                }
                return questionIds.size();
            }
        }
        for(JavaError error : JavaError.values()) {
            if(error.getErrorName().equals(keyword)) {
                Set<Integer> questionIds = ConcurrentHashMap.newKeySet();
                for (String key : error.getKeywords()) {
                    List<Question> matchingQuestions = questionRepository.findQuestionsByBodyContainingIgnoreCaseOrTitleContainingIgnoreCaseOrTagContainingIgnoreCase(
                            key, key, key);
                    matchingQuestions.forEach(question -> questionIds.add(question.getId()));
                }
                return questionIds.size();
            }
        }
        return questionRepository.findQuestionsByBodyContainingIgnoreCaseOrTitleContainingIgnoreCaseOrTagContainingIgnoreCase(
                keyword, keyword, keyword).size();
    }

    @Override
    public Map<String, Long> getDataSize() {
        return Map.of("answer", answerRepository.count(),
                "question", questionRepository.count(),
                "user", userRepository.count());
    }

    private Map<String, Set<Integer>> getTopicQuestionMap() {
        Map<String, Set<Integer>> topicQuestionMap = new ConcurrentHashMap<>();
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
        try {
            executorService.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread execution interrupted", e);
        } finally {
            executorService.shutdown();
        }
        return topicQuestionMap;
    }

    @Override
    public Map<String, Integer> getTopics(int n) {
        Map<String, Set<Integer>> topicQuestionMap = new ConcurrentHashMap<>(getTopicQuestionMap());

        return topicQuestionMap.entrySet()
                .stream()
                .sorted((entry1, entry2) -> Integer.compare(entry2.getValue().size(), entry1.getValue().size()))
                .limit(n)// Sorting by size in descending order
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().size(),
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    @Override
    public Map<String, Integer> getErrors(int n) {
        Map<String, Set<Integer>> errorQuestionMap = new ConcurrentHashMap<>();
        ExecutorService executorService = Executors.newFixedThreadPool(16);
        List<Callable<Void>> tasks = new ArrayList<>();
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
        return errorQuestionMap.entrySet()
                .stream()
                .sorted((entry1, entry2) -> Integer.compare(entry2.getValue().size(), entry1.getValue().size()))
                .limit(n)// Sorting by size in descending order
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().size(),
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    @Override
    public Map<String, Integer> getProTopics(int n) {
        Map<String, Integer> proTopics = new HashMap<>();
        // 获取 pro 用户
        Set<User> proUsers = new HashSet<>(userRepository.findUsersByScoreGreaterThan(100));
        List<Answer> proAnswers = answerRepository.findAnswersByOwnerUserIsIn(proUsers);
        Map<String, Set<Integer>> topicQuestionMap = getTopicQuestionMap();
        // 获取 pro 用户回答的所有问题 ID
        List<Integer> proQuestions = proAnswers.stream()
                .map(Answer::getQuestion)
                .map(Question::getId)
                .toList();

        ExecutorService executorService = Executors.newFixedThreadPool(8);
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
        return Collections.unmodifiableMap(
                proTopics.entrySet()
                        .stream()
                        .sorted((entry1, entry2) -> Integer.compare(entry2.getValue(), entry1.getValue()))
                        .limit(n)// 降序排序
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (e1, e2) -> e1,
                                LinkedHashMap::new // 保持插入顺序
                        ))
        );
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
                } else if (range.equals("Above 20")) {
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

        return convertToPercentage(scoreRangeMap);
    }

    @Override
    public Map<String, Integer> getQuestionsDistribution() {
        List<Object[]> result = questionRepository.findQuestionsScoreRanges();
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
                } else if (range.equals("Above 10")) {
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

        return convertToPercentage(scoreRangeMap);
    }

    @Override
    public Map<String, Integer> getAnswersDistribution() {
        List<Object[]> result = answerRepository.findAnswersScoreRanges();
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
                } else if (range.equals("Above 10")) {
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

        return convertToPercentage(scoreRangeMap);
    }


    public Map<String, Integer> convertToPercentage(Map<String, Integer> scores) {
        int total = scores.values().stream().mapToInt(Integer::intValue).sum();
        scores.replaceAll((range, count) -> (int) Math.round((count * 100.0) / total));
        return scores;
    }

    @Override
    public Map<String, Double> getAverageUserScores() {
        // 调用 Repository 层方法获取结果
        List<Object[]> queryResults = answerRepository.findAverageUserScores();

        // 解析查询结果
        Map<String, Double> result = new HashMap<>();
        if (!queryResults.isEmpty()) {
            Object[] row = queryResults.get(0); // 查询只返回一行
            result.put("avgUserScore", row[0] != null ? ((Number) row[0]).doubleValue() : null);
            result.put("avgUserScoreAbove10", row[1] != null ? ((Number) row[1]).doubleValue() : null);
        }
        return result;
    }

    @Override
    public Map<String, Map<String, Double>> calculateResponseTimeMetrics() {
        // 定义时间范围
        String[] intervals = {"3 days", "1 week", "1 month", "3 months", "6 months", "1 year"};

        // 用于存储结果的 Map
        Map<String, Map<String, Double>> result = new LinkedHashMap<>();

        for (String interval : intervals) {
            // 计算普通平均响应时间
            Double simpleAvg = answerRepository.findAvgResponseTimeWithin(interval, null);

            // 计算高分回答平均响应时间（分数 > 10）
            Double proAvg = answerRepository.findAvgResponseTimeWithin(interval, 10);

            // 存储到结果中
            Map<String, Double> metrics = new LinkedHashMap<>();
            metrics.put("simpleAvg", simpleAvg);
            metrics.put("proAvg", proAvg);
            result.put(interval, metrics);
        }

        return result;
    }

    @Override
    public Map<String, Double> getAverageScoresByLengthRange() {
        List<Object[]> results = answerRepository.findScoreAverageByLengthRange();

        // Convert raw query results to a sorted map
        return results.stream()
                .collect(
                        // Collect into a LinkedHashMap to maintain insertion order
                        Collectors.toMap(
                                result -> (String) result[0],       // Key: length range
                                result -> ((Number) result[1]).doubleValue(),       // Value: average score
                                (existing, replacement) -> existing, // Merge function (not needed here)
                                LinkedHashMap::new                  // Use LinkedHashMap to preserve order
                        )
                );
    }

    @Override
    public Map<String, Double> getAvgAnswerScoresByUserScoreRange() {
        List<Object[]> results = answerRepository.findAvgAnswerScoresByUserScoreRange();

        //转换为Map
        return results.stream()
                .collect(
                        Collectors.toMap(
                                result -> String.valueOf(result[0]),       // Key: user score range
                                result -> ((Number) result[1]).doubleValue(),       // Value: average answer score
                                (existing, replacement) -> existing, // Merge function (not needed here)
                                HashMap::new
                        )
                );
    }
}