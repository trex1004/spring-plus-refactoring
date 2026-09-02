package org.example.expert.domain.log.repository;

import org.example.expert.domain.log.entity.ManagerAssignmentLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ManagerAssignmentLogRepository extends JpaRepository<ManagerAssignmentLog, Long> {


    Optional<ManagerAssignmentLog> findByRequesterIdAndTodoIdAndManagerUserId(
            Long requesterId, Long todoId, Long managerUserId
    );
}
