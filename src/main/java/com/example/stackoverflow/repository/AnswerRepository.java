package com.example.stackoverflow.repository;

import com.example.stackoverflow.model.Answer;
import com.example.stackoverflow.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface AnswerRepository extends JpaRepository<Answer, Integer> {
    List<Answer> findAnswerByBodyContaining(String keyword);

    List<Answer> findAnswersByOwnerUserIsIn(Set<User> proUsers);
}