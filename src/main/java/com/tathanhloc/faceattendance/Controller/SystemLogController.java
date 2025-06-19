package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.*;
import com.tathanhloc.faceattendance.Service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class SystemLogController {

    private final SystemLogService systemLogService;

    @GetMapping
    public List<SystemLogDTO> getAll() {
        return systemLogService.getAll();
    }

    @GetMapping("/{id}")
    public SystemLogDTO getById(@PathVariable Long id) {
        return systemLogService.getById(id);
    }

    @PostMapping
    public SystemLogDTO create(@RequestBody SystemLogDTO dto) {
        return systemLogService.create(dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        systemLogService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
