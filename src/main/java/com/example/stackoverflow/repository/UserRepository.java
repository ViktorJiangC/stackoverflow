package com.example.stackoverflow.repository;

import com.example.stackoverflow.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Map;

public interface UserRepository extends JpaRepository<User, Integer> {
    @Query(value = "SELECT " +
            "   CASE " +
            "       WHEN score < 0 THEN 'Below 0' " +
            "       WHEN score >= 100 THEN 'Above 100' " +
            "       ELSE CONCAT('Between ', FLOOR(score / 10) * 10, ' and ', (FLOOR(score / 10) + 1) * 10) " +
            "   END AS score_range, " +
            "   COUNT(*) AS user_count " +
            "FROM users " +
            "GROUP BY score_range " +
            "ORDER BY score_range", nativeQuery = true)
    List<Object[]> findUserScoreRanges();

    List<User> findUsersByScoreGreaterThan(Integer score);
}