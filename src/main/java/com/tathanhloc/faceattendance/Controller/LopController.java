package com.tathanhloc.faceattendance.Controller;
import com.tathanhloc.faceattendance.DTO.LopDTO;
import com.tathanhloc.faceattendance.Service.LopService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lop")
@RequiredArgsConstructor
public class LopController {

    private final LopService lopService;

    // Lấy tất cả lớp (bao gồm cả đã xóa)
    @GetMapping("/all")
    public List<LopDTO> getAll() {
        return lopService.getAll();
    }

    // Lấy chỉ lớp đang hoạt động
    @GetMapping
    public List<LopDTO> getAllActive() {
        return lopService.getAllActive();
    }

    // Lấy chỉ lớp đã bị xóa mềm
    @GetMapping("/deleted")
    public List<LopDTO> getAllDeleted() {
        return lopService.getAllDeleted();
    }

    @GetMapping("/{id}")
    public LopDTO getById(@PathVariable String id) {
        return lopService.getById(id);
    }

    @PostMapping
    public LopDTO create(@RequestBody LopDTO dto) {
        return lopService.create(dto);
    }

    @PutMapping("/{id}")
    public LopDTO update(@PathVariable String id, @RequestBody LopDTO dto) {
        return lopService.update(id, dto);
    }

    // Xóa mềm
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(@PathVariable String id) {
        lopService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    // Khôi phục lớp đã xóa mềm
    @PutMapping("/{id}/restore")
    public ResponseEntity<LopDTO> restore(@PathVariable String id) {
        LopDTO restored = lopService.restore(id);
        return ResponseEntity.ok(restored);
    }

    // Xóa vĩnh viễn
    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDelete(@PathVariable String id) {
        lopService.hardDelete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-malop/{maLop}")
    public ResponseEntity<LopDTO> getByMaLop(@PathVariable String maLop) {
        return ResponseEntity.ok(lopService.getByMaLop(maLop));
    }

    @GetMapping("/count")
    public ResponseEntity<Long> count() {
        long count = lopService.count();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/count/active")
    public ResponseEntity<Long> countActive() {
        long count = lopService.countActive();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/count/inactive")
    public ResponseEntity<Long> countInactive() {
        long count = lopService.countInactive();
        return ResponseEntity.ok(count);
    }
}