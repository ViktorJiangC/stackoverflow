package com.example.stackoverflow.repository;

import com.example.stackoverflow.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Integer> {
    List<Question> findQuestionsByBodyContainingIgnoreCaseOrTitleContainingIgnoreCaseOrTagContainingIgnoreCase(String body, String title, String tag);

    List<Question> findQuestionsByClosedDateIsNotNullAndCreationDateIsNotNull();

    @Query(value = "SELECT " +
            "   CASE " +
            "       WHEN score < 0 THEN 'Below 0' " +
            "       WHEN score >= 10 THEN 'Above 10' " +
            "       ELSE CONCAT('Between ', FLOOR(score / 2) * 2, ' and ', (FLOOR(score / 2) + 1) * 2) " +
            "   END AS score_range, " +
            "   COUNT(*) AS user_count " +
            "FROM questions " +
            "GROUP BY score_range " +
            "ORDER BY score_range", nativeQuery = true)
    List<Object[]> findQuestionsScoreRanges();
}