package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.KhoaHocDTO;
import com.tathanhloc.faceattendance.Service.KhoaHocService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/khoahoc")
@RequiredArgsConstructor
public class KhoaHocController {

    private final KhoaHocService khoaHocService;

    @GetMapping
    public List<KhoaHocDTO> getAll() {
        return khoaHocService.getAll();
    }

    @GetMapping("/{id}")
    public KhoaHocDTO getById(@PathVariable String id) {
        return khoaHocService.getById(id);
    }

    @PostMapping
    public KhoaHocDTO create(@RequestBody KhoaHocDTO dto) {
        return khoaHocService.create(dto);
    }

    @PutMapping("/{id}")
    public KhoaHocDTO update(@PathVariable String id, @RequestBody KhoaHocDTO dto) {
        return khoaHocService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        khoaHocService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-makhoahoc/{maKhoahoc}")
    public ResponseEntity<KhoaHocDTO> getByMaKhoahoc(@PathVariable String maKhoahoc) {
        return ResponseEntity.ok(khoaHocService.getByMaKhoahoc(maKhoahoc));
    }

}
