package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.PhongHocDTO;
import com.tathanhloc.faceattendance.Service.PhongHocService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/phonghoc")
@RequiredArgsConstructor
public class PhongHocController {

    private final PhongHocService phongHocService;

    @GetMapping
    public ResponseEntity<Page<PhongHocDTO>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "maPhong") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(phongHocService.getAllWithPagination(page, size, sortBy, sortDir));
    }

    @GetMapping("/all")
    public ResponseEntity<List<PhongHocDTO>> getAllRooms() {
        return ResponseEntity.ok(phongHocService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PhongHocDTO> getById(@PathVariable String id) {
        return ResponseEntity.ok(phongHocService.getById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<PhongHocDTO>> searchRooms(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(phongHocService.searchRooms(keyword, page, size));
    }

    @GetMapping("/filter/type")
    public ResponseEntity<Page<PhongHocDTO>> filterByType(
            @RequestParam String loaiPhong,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(phongHocService.filterByType(loaiPhong, page, size));
    }

    @GetMapping("/filter/status")
    public ResponseEntity<Page<PhongHocDTO>> filterByStatus(
            @RequestParam String trangThai,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(phongHocService.filterByStatus(trangThai, page, size));
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Long>> getStatistics() {
        return ResponseEntity.ok(phongHocService.getRoomStatistics());
    }

    @PostMapping
    public ResponseEntity<PhongHocDTO> create(@RequestBody PhongHocDTO dto) {
        return ResponseEntity.ok(phongHocService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PhongHocDTO> update(@PathVariable String id, @RequestBody PhongHocDTO dto) {
        return ResponseEntity.ok(phongHocService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        phongHocService.delete(id);
        return ResponseEntity.noContent().build();
    }
}