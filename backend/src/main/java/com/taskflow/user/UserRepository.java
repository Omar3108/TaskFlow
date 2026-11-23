package com.taskflow.user;

import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByDemoExpiresAtAfter(Instant now);

    /**
     * Deletes expired demo accounts in one statement. The database removes what hangs from them
     * (memberships, refresh tokens) through ON DELETE CASCADE and unlinks cards and comments
     * through ON DELETE SET NULL; their workspaces must be deleted first (see DemoCleanup).
     */
    @Modifying
    @Query("delete from User u where u.demoExpiresAt < :now")
    int deleteExpiredDemoAccounts(@Param("now") Instant now);
}
