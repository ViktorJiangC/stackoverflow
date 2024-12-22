package com.example.stackoverflow.repository;

import com.example.stackoverflow.model.Answer;
import com.example.stackoverflow.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface AnswerRepository extends JpaRepository<Answer, Integer> {
    List<Answer> findAnswerByBodyContaining(String keyword);

    List<Answer> findAnswersByOwnerUserIsIn(Set<User> proUsers);

    @Query(value = "SELECT " +
            "   CASE " +
            "       WHEN score < 0 THEN 'Below 0' " +
            "       WHEN score >= 10 THEN 'Above 10' " +
            "       ELSE CONCAT('Between ', FLOOR(score / 2) * 2, ' and ', (FLOOR(score / 2) + 1) * 2) " +
            "   END AS score_range, " +
            "   COUNT(*) AS user_count " +
            "FROM answers " +
            "GROUP BY score_range " +
            "ORDER BY score_range", nativeQuery = true)
    List<Object[]> findAnswersScoreRanges();

    @Query(value = "SELECT AVG(u.score) AS avg_user_score, " +
            "(SELECT AVG(u.score) FROM users u JOIN answers a ON a.owneruserid = u.id WHERE a.score > 10 AND u.score IS NOT NULL) AS avg_user_score_above_10 " +
            "FROM users u JOIN answers a ON a.owneruserid = u.id WHERE a.score IS NOT NULL AND u.score IS NOT NULL", nativeQuery = true)
    List<Object[]> findAverageUserScores();

    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (a.creationdate - q.creationdate)) / 3600) AS avg_response_time_hours " +
            "FROM answers a " +
            "JOIN questions q ON a.questionid = q.id " +
            "WHERE (:minScore IS NULL OR a.score > :minScore) " +
            "AND a.creationdate IS NOT NULL " +
            "AND q.creationdate IS NOT NULL " +
            "AND a.creationdate - q.creationdate <= CAST(:interval AS INTERVAL)",
            nativeQuery = true)
    Double findAvgResponseTimeWithin(@Param("interval") String interval, @Param("minScore") Integer minScore);

    @Query(value = """
        SELECT 
            CONCAT(FLOOR(LENGTH(body) / 2000) * 2000, '-', FLOOR(LENGTH(body) / 2000) * 2000 + 1999) AS length_range,
            AVG(score) AS avg_score
        FROM answers
        WHERE LENGTH(body) BETWEEN 0 AND 16000
          AND body IS NOT NULL
          AND score < 100
        GROUP BY FLOOR(LENGTH(body) / 2000)
        ORDER BY FLOOR(LENGTH(body) / 2000);
        """, nativeQuery = true)
    List<Object[]> findScoreAverageByLengthRange();

    @Query(value = """

            WITH ScoreQuantiles AS (
                  SELECT
                      score,
                      NTILE(10) OVER (ORDER BY score) AS score_bucket
                  FROM users
              ),
                   UserScoreRanges AS (
                       SELECT
                           sq.score_bucket,
                           MIN(sq.score) AS min_score,
                           MAX(sq.score) AS max_score
                       FROM ScoreQuantiles sq
                       GROUP BY sq.score_bucket
                   )
              SELECT
                  usr.score_bucket,
                  AVG(a.score) AS average_answer_score
              FROM UserScoreRanges usr
                       LEFT JOIN users u ON usr.min_score <= u.score AND usr.max_score >= u.score
                       LEFT JOIN answers a ON u.id = a.owneruserid
              GROUP BY usr.score_bucket
              ORDER BY usr.score_bucket;
        """, nativeQuery = true)
    List<Object[]> findAvgAnswerScoresByUserScoreRange();
}