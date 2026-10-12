package com.example.errorfreetext.scheduler;

import com.example.errorfreetext.service.TaskProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class TaskProcessingScheduler {

    private static final Logger log = LoggerFactory.getLogger(TaskProcessingScheduler.class);

    private final TaskProcessingService taskProcessingService;

    @Value("${app.scheduler.batch-size}")
    private int batchSize;

    public TaskProcessingScheduler(TaskProcessingService taskProcessingService) {
        this.taskProcessingService = taskProcessingService;
    }

    @Scheduled(fixedDelayString = "${app.scheduler.delay-ms}")
    public void processNewTasks() {
        List<UUID> taskIds = taskProcessingService.claimNewTasks(batchSize);
        if (taskIds.isEmpty()) {
            return;
        }
        log.info("Планировщик взял в работу {} задач", taskIds.size());
        for (UUID taskId : taskIds) {
            taskProcessingService.process(taskId);
        }
    }
}
