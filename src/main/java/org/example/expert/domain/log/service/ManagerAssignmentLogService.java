package org.example.expert.domain.log.service;

import lombok.RequiredArgsConstructor;
import org.example.expert.domain.log.entity.ManagerAssignmentLog;
import org.example.expert.domain.log.repository.ManagerAssignmentLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ManagerAssignmentLogService {

    private final ManagerAssignmentLogRepository logRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLog(Long requesterId, Long todoId, Long managerUserId) {
        logRepository.save(new ManagerAssignmentLog(requesterId, todoId, managerUserId));
    }
}
