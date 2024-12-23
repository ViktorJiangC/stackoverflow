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
       LIMIT :n;
    """, nativeQuery = true)
    List<Object[]> findTopNTags(int n);

    @Query(value = """
       WITH TagList AS(
           SELECT
               unnest(string_to_array(trim(both '<>' from q.tags), '><')) AS tag
           FROM answers a
                    JOIN public.users u on u.user_id = a.user_id
                    JOIN public.questions q on a.question_id = q.id
           WHERE u.reputation > 10000
       )
       SELECT tag, COUNT(*) AS count
       FROM TagList
       GROUP BY tag
       ORDER BY count DESC
       LIMIT :n;
    """, nativeQuery = true)
    List<Object[]> findTopNTagsByProUsers(int n);


    @Query(value = """
       WITH TagList AS (
           SELECT
               unnest(string_to_array(trim(both '<>' from tags), '><')) AS tag,
               100.0 * (q.score - (SELECT MIN(score) FROM questions)) / ((SELECT MAX(score) FROM questions) - (SELECT MIN(score) FROM questions)) as score_,
               100.0 * (view_count - (SELECT MIN(view_count) FROM questions)) / ((SELECT MAX(view_count) FROM questions) - (SELECT MIN(view_count) FROM questions)) as view_count_,
               100.0 * (answer_count - (SELECT MIN(answer_count) FROM questions)) / ((SELECT MAX(answer_count) FROM questions) - (SELECT MIN(answer_count) FROM questions)) as answer_count_
           FROM answers a
                    JOIN public.users u on u.user_id = a.user_id
                    JOIN public.questions q on a.question_id = q.id
           WHERE u.reputation > 10000
       )
       SELECT tag, SUM(score_ + view_count_ + answer_count_) AS total_score
       FROM TagList
       GROUP BY tag
       ORDER BY total_score DESC
       LIMIT :n;
    """, nativeQuery = true)
    List<Object[]> findTopNEngagedTagsByProUsers(int n);
}