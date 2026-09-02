package org.example.expert.domain.log.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "log")
@EntityListeners(AuditingEntityListener.class)
public class ManagerAssignmentLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long requesterId;

    @Column(nullable = false)
    private Long todoId;

    @Column(nullable = false)
    private Long managerUserId;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ManagerAssignmentLog(
            Long requesterId,
            Long todoId,
            Long managerUserId
    ) {
        this.requesterId = requesterId;
        this.todoId = todoId;
        this.managerUserId = managerUserId;
    }
}
