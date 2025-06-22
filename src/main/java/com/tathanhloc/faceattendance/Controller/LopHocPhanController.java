package com.tathanhloc.faceattendance.Controller;

import com.tathanhloc.faceattendance.DTO.*;
import com.tathanhloc.faceattendance.Service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lophocphan")
@RequiredArgsConstructor
public class LopHocPhanController {

    private final LopHocPhanService lopHocPhanService;

    @GetMapping
    public List<LopHocPhanDTO> getAll() {
        return lopHocPhanService.getAll();
    }

    @GetMapping("/{id}")
    public LopHocPhanDTO getById(@PathVariable String id) {
        return lopHocPhanService.getById(id);
    }

    @PostMapping
    public LopHocPhanDTO create(@RequestBody LopHocPhanDTO dto) {
        return lopHocPhanService.create(dto);
    }

    @PutMapping("/{id}")
    public LopHocPhanDTO update(@PathVariable String id, @RequestBody LopHocPhanDTO dto) {
        return lopHocPhanService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        lopHocPhanService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-malhp/{maLhp}")
    public ResponseEntity<LopHocPhanDTO> getByMaLhp(@PathVariable String maLhp) {
        return ResponseEntity.ok(lopHocPhanService.getByMaLhp(maLhp));
    }

}
