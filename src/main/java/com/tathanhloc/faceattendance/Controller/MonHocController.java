package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.*;
import com.tathanhloc.faceattendance.Service.MonHocService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/monhoc")
@RequiredArgsConstructor
@Slf4j
public class MonHocController {

    private final MonHocService monHocService;

    /**
     * Lấy tất cả môn học
     */
    @GetMapping
    public ResponseEntity<List<MonHocDTO>> getAll() {
        log.info("Lấy danh sách tất cả môn học");
        return ResponseEntity.ok(monHocService.getAll());
    }

    /**
     * Lấy môn học theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<MonHocDTO> getById(@PathVariable String id) {
        log.info("Lấy môn học với ID: {}", id);
        return ResponseEntity.ok(monHocService.getById(id));
    }

    /**
     * Lấy môn học theo mã môn học
     */
    @GetMapping("/by-mamh/{maMh}")
    public ResponseEntity<MonHocDTO> getByMaMh(@PathVariable String maMh) {
        log.info("Lấy môn học theo mã: {}", maMh);
        return ResponseEntity.ok(monHocService.getByMaMh(maMh));
    }

    /**
     * Tạo môn học mới
     */
    @PostMapping
    public ResponseEntity<MonHocDTO> create(@RequestBody MonHocDTO dto) {
        log.info("Tạo môn học mới: {}", dto);
        MonHocDTO created = monHocService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Cập nhật môn học
     */
    @PutMapping("/{id}")
    public ResponseEntity<MonHocDTO> update(@PathVariable String id, @RequestBody MonHocDTO dto) {
        log.info("Cập nhật môn học với ID {}: {}", id, dto);
        return ResponseEntity.ok(monHocService.update(id, dto));
    }

    /**
     * Xóa mềm môn học (soft delete)
     * Set isActive = false thay vì xóa khỏi database
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(@PathVariable String id) {
        log.info("Xóa mềm môn học với ID: {}", id);
        monHocService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Khôi phục môn học đã xóa mềm
     */
    @PutMapping("/{id}/restore")
    public ResponseEntity<Void> restore(@PathVariable String id) {
        log.info("Khôi phục môn học với ID: {}", id);
        monHocService.restore(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Xóa vĩnh viễn môn học (hard delete)
     */
    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<Void> hardDelete(@PathVariable String id) {
        log.info("Xóa vĩnh viễn môn học với ID: {}", id);
        monHocService.hardDelete(id);
        return ResponseEntity.noContent().build();
    }
}