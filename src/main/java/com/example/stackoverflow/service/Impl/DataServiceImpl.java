package com.example.stackoverflow.service.Impl;

import com.example.stackoverflow.model.Question;
import com.example.stackoverflow.repository.AnswerRepository;
import com.example.stackoverflow.repository.QuestionRepository;
import com.example.stackoverflow.repository.UserRepository;
import com.example.stackoverflow.service.DataService;
import com.example.stackoverflow.service.JavaError;
import com.example.stackoverflow.service.JavaTopic;
import lombok.Getter;
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

    @Override
    public Map<String, Integer> getTopics() {
        Map<String, Integer> topicQuestionCount = new ConcurrentHashMap<>(); // 使用线程安全的 ConcurrentHashMap
        ExecutorService executorService = Executors.newFixedThreadPool(8); // 创建一个包含 8 个线程的线程池
        List<Callable<Void>> tasks = new ArrayList<>();

        for (JavaTopic topic : JavaTopic.values()) {
            tasks.add(() -> {
                int count = 0;
                Set<Integer> processedQuestionIds = ConcurrentHashMap.newKeySet(); // 使用线程安全的 Set 来存储已处理的问题 ID

                for (String keyword : topic.getKeywords()) {
                    List<Question> matchingQuestions = questionRepository.findQuestionsByBodyContainingIgnoreCaseOrTitleContainingIgnoreCaseOrTagContainingIgnoreCase(
                            keyword, keyword, keyword);

                    // 对查询结果进行去重，只统计未处理过的问题
                    for (Question question : matchingQuestions) {
                        if (!processedQuestionIds.contains(question.getId())) {
                            processedQuestionIds.add(question.getId()); // 标记该问题 ID 为已处理
                            count++; // 增加计数
                        }
                    }
                }
                topicQuestionCount.put(topic.getTopicName(), count); // 将结果写入线程安全的 Map
                return null;
            });
        }

        try {
            // 提交所有任务并等待完成
            executorService.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 恢复中断状态
            throw new RuntimeException("Thread execution interrupted", e);
        } finally {
            executorService.shutdown(); // 关闭线程池
        }

        // 按值降序排序并返回结果
        return topicQuestionCount.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    @Override
    public Map<String, Integer> getErrors() {
        Map<String, Integer> errorsCount = new ConcurrentHashMap<>(); // 使用线程安全的 ConcurrentHashMap
        ExecutorService executorService = Executors.newFixedThreadPool(8); // 创建一个包含 8 个线程的线程池
        List<Callable<Void>> tasks = new ArrayList<>();


        for (JavaError error : JavaError.values()) {
            tasks.add(() -> {
                int count = 0;
                Set<Integer> processedQuestionIds = ConcurrentHashMap.newKeySet(); // 使用线程安全的 Set 来存储已处理的问题 ID

                for (String keyword : error.getKeywords()) {
                    // 并发查询问题数据
                    List<Question> matchingQuestions = questionRepository.findQuestionsByBodyContainingIgnoreCaseOrTitleContainingIgnoreCaseOrTagContainingIgnoreCase(
                            keyword, keyword, keyword);

                    // 对查询结果进行去重，只统计未处理过的问题
                    for (Question question : matchingQuestions) {
                        if (!processedQuestionIds.contains(question.getId())) {
                            processedQuestionIds.add(question.getId()); // 标记该问题 ID 为已处理
                            count++; // 增加计数
                        }
                    }
                }
                errorsCount.put(error.getErrorName(), count); // 将结果写入线程安全的 Map
                return null;
            });
        }

        try {
            // 提交所有任务并等待完成
            executorService.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 恢复中断状态
            throw new RuntimeException("Thread execution interrupted", e);
        } finally {
            executorService.shutdown(); // 关闭线程池
        }

        // 按值降序排序并返回结果
        return errorsCount.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

}