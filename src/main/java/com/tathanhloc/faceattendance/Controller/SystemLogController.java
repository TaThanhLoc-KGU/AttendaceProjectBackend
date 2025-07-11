package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.SystemLogDTO;
import com.tathanhloc.faceattendance.Model.SystemLog;
import com.tathanhloc.faceattendance.Service.SystemLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SystemLogController {

    private final SystemLogService logService;

    @GetMapping
    public ResponseEntity<Page<SystemLogDTO>> getAllLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<SystemLogDTO> logs = logService.getAllLogs(pageable);

        return ResponseEntity.ok(logs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SystemLogDTO> getLogById(@PathVariable Long id) {
        SystemLogDTO log = logService.getLogById(id);
        if (log != null) {
            return ResponseEntity.ok(log);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/search")
    public ResponseEntity<Page<SystemLogDTO>> searchLogs(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) SystemLog.LogLevel logLevel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<SystemLogDTO> logs = logService.searchLogs(
                module, logLevel, status, userId, startTime, endTime, keyword, pageable);

        return ResponseEntity.ok(logs);
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getLogStatistics() {
        Map<String, Object> stats = logService.getLogStatistics();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/recent-errors")
    public ResponseEntity<List<SystemLogDTO>> getRecentErrors(
            @RequestParam(defaultValue = "10") int limit) {

        List<SystemLogDTO> errors = logService.getRecentErrors(limit);
        return ResponseEntity.ok(errors);
    }

    @GetMapping("/slow-operations")
    public ResponseEntity<List<SystemLogDTO>> getSlowOperations(
            @RequestParam(defaultValue = "5000") long thresholdMs,
            @RequestParam(defaultValue = "10") int limit) {

        List<SystemLogDTO> slowOps = logService.getSlowOperations(thresholdMs, limit);
        return ResponseEntity.ok(slowOps);
    }

    @PostMapping("/test")
    public ResponseEntity<String> testLogging() {
        // Test different log levels
        logService.logInfo("TEST", "TEST_INFO", "This is a test info log");
        logService.logWarning("TEST", "TEST_WARNING", "This is a test warning log");
        logService.logError("TEST", "TEST_ERROR", "This is a test error log", "Stack trace details here");
        logService.logUserAction("TEST", "TEST_USER_ACTION", "User performed test action", "test_user", "Test User");
        logService.logPerformance("TEST", "TEST_PERFORMANCE", "Test performance operation", 2500L);

        return ResponseEntity.ok("Test logs created successfully");
    }

    @DeleteMapping("/cleanup")
    public ResponseEntity<String> cleanupOldLogs(
            @RequestParam(defaultValue = "30") int daysToKeep) {

        logService.cleanupOldLogs(daysToKeep);
        return ResponseEntity.ok("Old logs cleanup completed");
    }

    @GetMapping("/modules")
    public ResponseEntity<List<String>> getAvailableModules() {
        // Return common modules used in the system
        List<String> modules = List.of(
                "AUTHENTICATION", "USER", "STUDENT", "TEACHER",
                "ATTENDANCE", "CAMERA", "SYSTEM", "DATABASE", "API"
        );
        return ResponseEntity.ok(modules);
    }

    @GetMapping("/levels")
    public ResponseEntity<List<String>> getLogLevels() {
        List<String> levels = List.of("TRACE", "DEBUG", "INFO", "WARN", "ERROR", "FATAL");
        return ResponseEntity.ok(levels);
    }

    @GetMapping("/statuses")
    public ResponseEntity<List<String>> getStatuses() {
        List<String> statuses = List.of("SUCCESS", "FAILED", "WARNING");
        return ResponseEntity.ok(statuses);
    }
}