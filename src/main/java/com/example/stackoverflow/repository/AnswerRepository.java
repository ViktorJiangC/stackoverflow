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

    @Query(value = "SELECT AVG(u.reputation) AS avg_user_score, " +
            "(SELECT AVG(u.reputation) FROM users u JOIN answers a ON a.user_id = u.user_id WHERE a.score > 10 AND u.reputation IS NOT NULL) AS avg_user_score_above_10 " +
            "FROM users u JOIN answers a ON a.user_id = u.user_id WHERE a.score IS NOT NULL AND u.reputation IS NOT NULL", nativeQuery = true)
    List<Object[]> findAverageUserScores();

    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (a.creation_date - q.creation_date)) / 3600) AS avg_response_time_hours " +
            "FROM answers a " +
            "JOIN questions q ON a.question_id = q.id " +
            "WHERE (:minScore IS NULL OR a.score > :minScore) " +
            "AND a.creation_date IS NOT NULL " +
            "AND q.creation_date IS NOT NULL " +
            "AND a.creation_date - q.creation_date <= CAST(:interval AS INTERVAL)",
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
                       LEFT JOIN answers a ON u.id = a.user_id
              GROUP BY usr.score_bucket
              ORDER BY usr.score_bucket;
        """, nativeQuery = true)
    List<Object[]> findAvgAnswerScoresByUserScoreRange();

    @Query(value = """

        WITH AnswerTimeDifferences AS (
            SELECT
                a.score,
                a.creation_date AS answer_creation_date,
                q.creation_date AS question_creation_date,
                EXTRACT(EPOCH FROM (a.creation_date - q.creation_date)) AS time_difference_seconds
            FROM answers a
            JOIN questions q ON a.question_id = q.id
        ),
        Quantiles AS (
            SELECT
                score,
                NTILE(10) OVER (ORDER BY time_difference_seconds) AS time_bucket
            FROM AnswerTimeDifferences
        )
        SELECT
            q.time_bucket,
            AVG(q.score) AS average_answer_score
        FROM Quantiles q
        GROUP BY q.time_bucket
        ORDER BY q.time_bucket;
        """, nativeQuery = true)
    List<Object[]> findAvgAnswerScoresByTimeRange();

    @Query(
        value = """
            WITH AnswerLengthBuckets AS (
                SELECT
                    a.score,
                    LENGTH(a.body) AS body_length,
                    NTILE(10) OVER (ORDER BY LENGTH(a.body)) AS length_bucket
                FROM answers a
            )
            SELECT
                al.length_bucket,
                AVG(al.score) AS average_answer_score
            FROM AnswerLengthBuckets al
            GROUP BY al.length_bucket
            ORDER BY al.length_bucket;
        """, nativeQuery = true
    )
    List<Object[]> findAvgAnswerScoresByLengthRange();
}