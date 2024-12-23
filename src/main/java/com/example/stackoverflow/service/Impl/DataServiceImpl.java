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
        for(JavaError error : JavaError.values()) {
            if(error.getErrorName().equals(keyword)) {
                Set<Integer> questionIds = ConcurrentHashMap.newKeySet();
                for (String key : error.getKeywords()) {
                    List<Question> matchingQuestions = questionRepository.findQuestionsByBodyContainingIgnoreCaseOrTitleContainingIgnoreCaseOrTagsContainingIgnoreCase(
                            key);
                    matchingQuestions.forEach(question -> questionIds.add(question.getId()));
                }
                return questionIds.size();
            }
        }
        return questionRepository.findQuestionsByBodyContainingIgnoreCaseOrTitleContainingIgnoreCaseOrTagsContainingIgnoreCase(
                keyword).size();
    }

    @Override
    public Map<String, Long> getDataSize() {
        return Map.of("answer", answerRepository.count(),
                "question", questionRepository.count(),
                "user", userRepository.count());
    }

    @Override
    public Map<String, Integer> getTopics(int n) {
        List<Object[]> result = questionRepository.findTopNTags(n);
        return result.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).intValue(),
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
                    List<Question> matchingQuestions = questionRepository.findQuestionsByBodyContainingIgnoreCaseOrTitleContainingIgnoreCaseOrTagsContainingIgnoreCase(
                            keyword);
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
        List<Object[]> result = questionRepository.findTopNTagsByProUsers(n);
        return result.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).intValue(),
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    @Override
    public Map<String, Integer> getMostEngagedTopics(int n) {
        List<Object[]> result = questionRepository.findTopNEngagedTagsByProUsers(n);
        return result.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).intValue(),
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
                                result -> String.valueOf((Integer) result[0] - 1),       // Key: user score range
                                result -> ((Number) result[1]).doubleValue(),       // Value: average answer score
                                (existing, replacement) -> existing, // Merge function (not needed here)
                                HashMap::new
                        )
                );
    }

    @Override
    public Map<String, Double> getAvgAnswerScoresByTimeRange() {
        List<Object[]> results = answerRepository.findAvgAnswerScoresByTimeRange();
        //转换为Map
        return results.stream()
                .collect(
                        Collectors.toMap(
                                result -> String.valueOf((Integer) result[0] - 1),       // Key: time range
                                result -> ((Number) result[1]).doubleValue(),       // Value: average answer score
                                (existing, replacement) -> existing, // Merge function (not needed here)
                                HashMap::new
                        )
                );
    }

    @Override
    public Map<String, Double> getAvgAnswerScoresByLengthRange() {
        List<Object[]> results = answerRepository.findAvgAnswerScoresByLengthRange();
        //转换为Map
        return results.stream()
                .collect(
                        Collectors.toMap(
                                result -> String.valueOf((Integer) result[0] - 1),       // Key: length range
                                result -> ((Number) result[1]).doubleValue(),       // Value: average answer score
                                (existing, replacement) -> existing, // Merge function (not needed here)
                                HashMap::new
                        )
                );
    }

    @Override
    public Map<String, Double> getAcceptedRatesByUserScoreRange() {
        List<Object[]> results = answerRepository.findAcceptedRatesByUserScoreRange();
        //转换为Map
        return results.stream()
                .collect(
                        Collectors.toMap(
                                result -> String.valueOf((Integer) result[0] - 1),       // Key: user score range
                                result -> ((Number) result[1]).doubleValue(),       // Value: accepted rate
                                (existing, replacement) -> existing, // Merge function (not needed here)
                                HashMap::new
                        )
                );
    }

    @Override
    public Map<String, Double> getAcceptedRatesByTimeRange() {
        List<Object[]> results = answerRepository.findAcceptedRatesByTimeRange();
        //转换为Map
        return results.stream()
                .collect(
                        Collectors.toMap(
                                result -> String.valueOf((Integer) result[0] - 1),       // Key: time range
                                result -> ((Number) result[1]).doubleValue(),       // Value: accepted rate
                                (existing, replacement) -> existing, // Merge function (not needed here)
                                HashMap::new
                        )
                );
    }

    @Override
    public Map<String, Double> getAcceptedRatesByLengthRange() {
        List<Object[]> results = answerRepository.findAcceptedRatesByLengthRange();
        //转换为Map
        return results.stream()
                .collect(
                        Collectors.toMap(
                                result -> String.valueOf((Integer) result[0] - 1),       // Key: length range
                                result -> ((Number) result[1]).doubleValue(),       // Value: accepted rate
                                (existing, replacement) -> existing, // Merge function (not needed here)
                                HashMap::new
                        )
                );
    }
}