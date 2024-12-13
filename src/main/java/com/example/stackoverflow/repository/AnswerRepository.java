package com.example.stackoverflow.repository;

import com.example.stackoverflow.model.Answer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnswerRepository extends JpaRepository<Answer, Integer> {
    List<Answer> findAnswerByBodyContaining(String keyword);
}