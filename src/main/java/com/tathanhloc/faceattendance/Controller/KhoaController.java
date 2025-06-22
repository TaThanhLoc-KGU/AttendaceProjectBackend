package com.tathanhloc.faceattendance.Controller;
import com.tathanhloc.faceattendance.DTO.KhoaDTO;
import com.tathanhloc.faceattendance.Service.KhoaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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


}
