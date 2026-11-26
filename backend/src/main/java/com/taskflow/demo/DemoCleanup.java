package com.taskflow.demo;

import com.taskflow.user.UserRepository;
import com.taskflow.workspace.WorkspaceRepository;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deletes demo sandboxes once they expire. Runs every hour, and also before a new sandbox is
 * created: on free hosting the server sleeps when idle, so a timer alone could miss days.
 */
@Component
public class DemoCleanup {

    private static final Logger log = LoggerFactory.getLogger(DemoCleanup.class);

    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public DemoCleanup(WorkspaceRepository workspaceRepository, UserRepository userRepository, Clock clock) {
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT5M")
    @Transactional
    public void deleteExpired() {
        Instant now = clock.instant();
        // Workspaces first: deleting the users would leave their workspaces without an owner.
        int workspaces = workspaceRepository.deleteOwnedByExpiredDemoAccounts(now);
        int users = userRepository.deleteExpiredDemoAccounts(now);
        if (users > 0) {
            log.info("Deleted {} expired demo accounts and {} workspaces", users, workspaces);
        }
    }
}
