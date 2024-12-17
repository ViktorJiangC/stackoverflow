package com.example.stackoverflow.repository;

import com.example.stackoverflow.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Integer> {
    List<Question> findQuestionsByBodyContainingIgnoreCaseOrTitleContainingIgnoreCaseOrTagContainingIgnoreCase(String body, String title, String tag);

    List<Question> findQuestionsByClosedDateIsNotNullAndCreationDateIsNotNull();
}