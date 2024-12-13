package com.example.stackoverflow.model;

import jakarta.persistence.*;
import lombok.Data;
import java.sql.Timestamp;

@Data
@Entity
@Table(name = "answers")
public class Answer {

    @Id
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "owneruserid")
    private User ownerUser;

    private Timestamp creationDate;

    @ManyToOne
    @JoinColumn(name = "questionid")
    private Question question;

    private Integer score;
    @Column(name = "body", columnDefinition = "TEXT")
    private String body;
    // Getters and Setters
}