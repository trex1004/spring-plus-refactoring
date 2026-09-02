package org.example.expert.domain.manager.service;

import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.log.entity.ManagerAssignmentLog;
import org.example.expert.domain.log.repository.ManagerAssignmentLogRepository;
import org.example.expert.domain.manager.dto.request.ManagerSaveRequest;
import org.example.expert.domain.manager.repository.ManagerRepository;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.todo.repository.TodoRepository;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@SpringBootTest
class ManagerServiceTest {

    @Autowired
    private ManagerService managerService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TodoRepository todoRepository;
    @Autowired
    private ManagerRepository managerRepository;
    @Autowired
    private ManagerAssignmentLogRepository managerAssignmentLogRepository;

    @Test
    void 담장자_등록에_실패해도_요청_로그는_저장된다() {

        // given
        User user = userRepository.save
                (new User(
                        "test@example.com",
                        "password",
                        "곽한구",
                        UserRole.USER));

        Todo todo = todoRepository.save(
                new Todo("title", "contents", "Sunny", user));
        AuthUser authUser = new AuthUser(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getUserRole()
        );

        long managerCountBefore = managerRepository.count();
        long logCountBefore = managerAssignmentLogRepository.count();
        long invalidManagerUserId = 999L;

        // when
        Throwable throwable = catchThrowable(() -> managerService.saveManager(
                authUser, todo.getId(), new ManagerSaveRequest(invalidManagerUserId)
        ));

        // then
        ManagerAssignmentLog log =  managerAssignmentLogRepository.findByRequesterIdAndTodoIdAndManagerUserId(
                user.getId(),todo.getId(),invalidManagerUserId).orElseThrow();
        assertThat(throwable).isInstanceOf(InvalidRequestException.class);
        assertThat(managerRepository.count()).isEqualTo(managerCountBefore);
        assertThat(managerAssignmentLogRepository.count()).isEqualTo(logCountBefore + 1);
        assertThat(log.getRequesterId()).isEqualTo(user.getId());
        assertThat(log.getTodoId()).isEqualTo(todo.getId());
        assertThat(log.getManagerUserId()).isEqualTo(invalidManagerUserId);
        assertThat(log.getCreatedAt()).isNotNull();
    }

}

