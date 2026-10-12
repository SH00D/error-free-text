package com.example.errorfreetext.repository;

import com.example.errorfreetext.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    @Query(value = """
            SELECT * FROM tasks
            WHERE status = 'NEW'
            ORDER BY created_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<Task> findNewTasksForUpdate(@Param("limit") int limit);

    @Modifying
    @Query(value = "UPDATE tasks SET status = 'NEW', updated_at = now() WHERE status = 'IN_PROGRESS'",
            nativeQuery = true)
    int resetInProgressTasks();
}
