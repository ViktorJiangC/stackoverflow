package com.example.stackoverflow.model;

import jakarta.persistence.*;
import lombok.Data;
import java.sql.Timestamp;

@Data
@Entity
@Table(name = "questions")
public class Question {
    @Id
    private Integer id;
    private Timestamp creationDate;
    private Timestamp closedDate;
    private Integer score;
    private Integer viewCount;

    private String title;
    @Column(name = "body_text", columnDefinition = "TEXT")
    private String body;
    private String tags;
}