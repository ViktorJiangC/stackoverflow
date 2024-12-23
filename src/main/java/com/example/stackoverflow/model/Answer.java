package com.example.stackoverflow.model;

import jakarta.persistence.*;
import lombok.Data;
import java.sql.Timestamp;

@Data
@Entity
@Table(name = "answers")
public class Answer {

    @Id
    @Column(name = "answer_id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User ownerUser;

    private Timestamp creationDate;

    @ManyToOne
    @JoinColumn(name = "question_id")
    private Question question;

    private Integer score;

    private Boolean isAccepted;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;
    // Getters and Setters
}