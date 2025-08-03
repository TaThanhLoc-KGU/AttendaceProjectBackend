package com.tathanhloc.faceattendance.Repository;

import com.tathanhloc.faceattendance.Model.SystemLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {

    // Tìm theo level
    Page<SystemLog> findByLogLevel(SystemLog.LogLevel logLevel, Pageable pageable);

    // Tìm theo module
    Page<SystemLog> findByModule(String module, Pageable pageable);

    // Tìm theo user
    Page<SystemLog> findByUserId(String userId, Pageable pageable);

    // Tìm theo khoảng thời gian
    Page<SystemLog> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    // Tìm theo status
    Page<SystemLog> findByStatus(String status, Pageable pageable);

    // Search với nhiều điều kiện
    @Query("SELECT l FROM SystemLog l WHERE " +
            "(:module IS NULL OR l.module = :module) AND " +
            "(:logLevel IS NULL OR l.logLevel = :logLevel) AND " +
            "(:status IS NULL OR l.status = :status) AND " +
            "(:userId IS NULL OR l.userId = :userId) AND " +
            "(:startTime IS NULL OR l.createdAt >= :startTime) AND " +
            "(:endTime IS NULL OR l.createdAt <= :endTime) AND " +
            "(:keyword IS NULL OR LOWER(l.message) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " LOWER(l.action) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<SystemLog> findWithFilters(
            @Param("module") String module,
            @Param("logLevel") SystemLog.LogLevel logLevel,
            @Param("status") String status,
            @Param("userId") String userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // Thống kê theo level
    @Query("SELECT l.logLevel, COUNT(l) FROM SystemLog l GROUP BY l.logLevel")
    List<Object[]> countByLogLevel();

    // Thống kê theo module
    @Query("SELECT l.module, COUNT(l) FROM SystemLog l GROUP BY l.module ORDER BY COUNT(l) DESC")
    List<Object[]> countByModule();

    // Thống kê theo status
    @Query("SELECT l.status, COUNT(l) FROM SystemLog l WHERE l.status IS NOT NULL GROUP BY l.status")
    List<Object[]> countByStatus();

    // Logs trong 24h qua
    @Query("SELECT COUNT(l) FROM SystemLog l WHERE l.createdAt >= :since")
    Long countSince(@Param("since") LocalDateTime since);

    // Logs lỗi trong khoảng thời gian
    @Query("SELECT COUNT(l) FROM SystemLog l WHERE l.logLevel IN ('ERROR', 'FATAL') AND l.createdAt >= :since")
    Long countErrorsSince(@Param("since") LocalDateTime since);

    // Top users có nhiều log nhất
    @Query("SELECT l.userId, l.userName, COUNT(l) FROM SystemLog l WHERE l.userId IS NOT NULL " +
            "GROUP BY l.userId, l.userName ORDER BY COUNT(l) DESC")
    List<Object[]> getTopUsers(Pageable pageable);

    // Recent error logs
    @Query("SELECT l FROM SystemLog l WHERE l.logLevel IN ('ERROR', 'FATAL') ORDER BY l.createdAt DESC")
    List<SystemLog> findRecentErrors(Pageable pageable);

    // System performance logs
    @Query("SELECT l FROM SystemLog l WHERE l.durationMs IS NOT NULL AND l.durationMs > :threshold ORDER BY l.durationMs DESC")
    List<SystemLog> findSlowOperations(@Param("threshold") Long threshold, Pageable pageable);

    // Delete old logs
    @Query("DELETE FROM SystemLog l WHERE l.createdAt < :cutoffDate")
    void deleteOldLogs(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Count logs by date range
    @Query("SELECT DATE(l.createdAt), COUNT(l) FROM SystemLog l " +
            "WHERE l.createdAt >= :startDate AND l.createdAt <= :endDate " +
            "GROUP BY DATE(l.createdAt) ORDER BY DATE(l.createdAt)")
    List<Object[]> countLogsByDate(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);
}