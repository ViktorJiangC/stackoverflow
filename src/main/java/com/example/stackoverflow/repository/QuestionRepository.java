package com.example.stackoverflow.repository;

import com.example.stackoverflow.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Integer> {
    List<Question> findQuestionsByBodyContainingIgnoreCaseOrTitleContainingIgnoreCaseOrTagsContainingIgnoreCase(String body, String title, String tag);

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

    @Query(value = """
       WITH TagList AS (
           SELECT
               unnest(string_to_array(trim(both '<>' from tags), '><')) AS tag
           FROM questions
       )
       SELECT tag, COUNT(*) AS count
       FROM TagList
       GROUP BY tag
       ORDER BY count DESC
       LIMIT :n
       OFFSET 1;
    """, nativeQuery = true)
    List<Object[]> findTopNTags(int n);
}