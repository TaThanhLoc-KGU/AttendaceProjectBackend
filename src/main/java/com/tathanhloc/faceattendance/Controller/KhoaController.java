package com.tathanhloc.faceattendance.Controller;
import com.tathanhloc.faceattendance.DTO.KhoaDTO;
import com.tathanhloc.faceattendance.Service.KhoaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/khoa")
@RequiredArgsConstructor
public class KhoaController {

    private final KhoaService khoaService;

    @GetMapping
    public List<KhoaDTO> getAll() {
        return khoaService.getAll();
    }

    @GetMapping("/{id}")
    public KhoaDTO getById(@PathVariable String id) {
        return khoaService.getById(id);
    }

    @PostMapping
    public KhoaDTO create(@RequestBody KhoaDTO dto) {
        return khoaService.create(dto);
    }

    @PutMapping("/{id}")
    public KhoaDTO update(@PathVariable String id, @RequestBody KhoaDTO dto) {
        return khoaService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        khoaService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/by-makhoa/{maKhoa}")
    public ResponseEntity<KhoaDTO> getByMaKhoa(@PathVariable String maKhoa) {
        return ResponseEntity.ok(khoaService.getByMaKhoa(maKhoa));
    }
    @GetMapping("/active")
    public ResponseEntity<List<KhoaDTO>> getAllActive() {
        try {
            List<KhoaDTO> activeKhoas = khoaService.getActiveKhoas(); // Đổi tên method
            return ResponseEntity.ok(activeKhoas);
        } catch (Exception e) {
            System.err.println("❌ Error getting active khoa list: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }
}
