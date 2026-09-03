package org.example.expert.domain.todo.repository;

import org.example.expert.domain.todo.entity.Todo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface TodoRepository extends JpaRepository<Todo, Long>, TodoQueryRepository {

    @Query(
            value = """
                    SELECT t
                    FROM Todo t
                    JOIN FETCH t.user
                    WHERE (:weather IS NULL OR t.weather = :weather)
                    AND (:modifiedAtStart IS NULL OR t.modifiedAt >= :modifiedAtStart)
                    AND (:modifiedAtEnd IS NULL OR t.modifiedAt <= :modifiedAtEnd)
                    ORDER BY t.modifiedAt DESC
                    """,
            countQuery = """
                    SELECT COUNT(t)
                    FROM Todo t
                    WHERE (:weather IS NULL OR t.weather = :weather)
                    AND (:modifiedAtStart IS NULL OR t.modifiedAt >= :modifiedAtStart)
                    AND (:modifiedAtEnd IS NULL OR t.modifiedAt <= :modifiedAtEnd)
                    """)
    Page<Todo> searchTodos(@Param("weather") String weather,
                           @Param("modifiedAtStart") LocalDateTime modifiedAtStart,
                           @Param("modifiedAtEnd") LocalDateTime modifiedAtEnd,
                           Pageable pageable);

}
