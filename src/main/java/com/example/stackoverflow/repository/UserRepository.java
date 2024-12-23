package com.example.stackoverflow.repository;

import com.example.stackoverflow.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Map;

public interface UserRepository extends JpaRepository<User, Integer> {
    @Query(value = "SELECT " +
            "   CASE " +
            "       WHEN reputation < 0 THEN 'Below 0' " +
            "       WHEN reputation >= 20 THEN 'Above 20' " +
            "       ELSE CONCAT('Between ', FLOOR(reputation / 4) * 4, ' and ', (FLOOR(reputation / 4) + 1) * 4) " +
            "   END AS score_range, " +
            "   COUNT(*) AS user_count " +
            "FROM users " +
            "GROUP BY score_range " +
            "ORDER BY score_range", nativeQuery = true)
    List<Object[]> findUserScoreRanges();

    List<User> findUsersByScoreGreaterThan(Integer score);
}