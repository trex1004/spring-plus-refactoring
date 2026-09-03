package org.example.expert.domain.todo.repository;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.expert.domain.todo.dto.request.TodoSearchCondition;
import org.example.expert.domain.todo.dto.response.TodoSearchResponse;
import org.example.expert.domain.todo.entity.Todo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.example.expert.domain.comment.entity.QComment.comment;
import static org.example.expert.domain.manager.entity.QManager.manager;
import static org.example.expert.domain.todo.entity.QTodo.todo;
import static org.example.expert.domain.user.entity.QUser.user;

@RequiredArgsConstructor
public class TodoQueryRepositoryImpl implements TodoQueryRepository {
private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Optional<Todo> findByIdWithUser(Long todoId) {
        Todo result = jpaQueryFactory
                .selectFrom(todo)
                .join(todo.user, user).fetchJoin()
                .where(todo.id.eq(todoId))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public Page<TodoSearchResponse> searchTodos(TodoSearchCondition condition, Pageable pageable) {
        BooleanExpression[] conditions = searchConditions(condition);

        List<TodoSearchResponse> content = jpaQueryFactory
                .select(Projections.constructor(
                        TodoSearchResponse.class,
                        todo.title,
                        managerCount(),
                        commentCount()
                ))
                .from(todo)
                .where(conditions)
                .orderBy(todo.createdAt.desc(), todo.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = jpaQueryFactory
                .select(todo.count())
                .from(todo)
                .where(conditions);
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanExpression[] searchConditions(TodoSearchCondition condition) {
        return new BooleanExpression[]{
                titleContains(condition.getTitle()),
                createdAtBetween(
                        condition.getCreatedAtStart(),
                        condition.getCreatedAtEnd()
                ),
                managerNicknameContains(condition.getManagerNickname())
        };
    }

    private BooleanExpression titleContains(String title) {
        return StringUtils.hasText(title) ? todo.title.contains(title) : null;
    }

    private BooleanExpression createdAtBetween(LocalDateTime createdAtStart, LocalDateTime createdAtEnd) {
        if (createdAtStart == null && createdAtEnd == null) {
            return  null;
        }
        if (createdAtStart == null) {
            return todo.createdAt.loe(createdAtEnd);
        }
        if (createdAtEnd == null) {
            return todo.createdAt.goe(createdAtStart);
        }
        return todo.createdAt.between(createdAtStart, createdAtEnd);
    }

    private BooleanExpression managerNicknameContains(String managerNickname) {
        if (!StringUtils.hasText(managerNickname)) {
            return null;
        }
        return JPAExpressions
                .selectOne()
                .from(manager)
                .join(manager.user, user)
                .where(
                        manager.todo.eq(todo),
                        user.nickname.contains(managerNickname)
                )
                .exists();
    }
    private Expression<Long> managerCount() {
        return JPAExpressions
                .select(manager.count())
                .from(manager)
                .where(manager.todo.eq(todo));
    }
    private Expression<Long> commentCount() {
        return JPAExpressions
                .select(comment.count())
                .from(comment)
                .where(comment.todo.eq(todo));
    }
}
