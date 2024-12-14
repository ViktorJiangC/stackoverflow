package com.example.stackoverflow.repository;

import com.example.stackoverflow.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Integer> {
    List<Question> findQuestionsByBodyContainingIgnoreCaseOrTitleNotContainingIgnoreCaseOrTagContainingIgnoreCase(String body, String title, String tag);
}